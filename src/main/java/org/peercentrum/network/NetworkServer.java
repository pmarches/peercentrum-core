package org.peercentrum.network;

import java.net.InetSocketAddress;
import java.util.Hashtable;

import javax.security.cert.X509Certificate;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class NetworkServer extends NetworkBase { //TODO implement AutoClosable
  private static final String TLS_HANDLER_NAME = "tls";
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkServer.class);
  protected static final AttributeKey<NodeIdentifier> REMOTE_NODE_ID_ATTR = AttributeKey.valueOf("REMOTE_NODE_ID");

  DefaultEventExecutorGroup applicationWorkerGroup = new DefaultEventExecutorGroup(2);
  NioEventLoopGroup nioWorkerGroup = new NioEventLoopGroup();
  Channel bindChannel;
  TopLevelConfig configuration;
  Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler> allApplicationHandler=new Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler>();
  protected int effectiveListeningPort;
  protected NodeDatabase nodeDatabase;
  protected ECDSASslContext serverSideSSLContext;
  public NetworkClient networkClient;

  public NetworkServer(TopLevelConfig config) throws Exception {
    super(new NodeIdentity(config));
    this.configuration=config;
//    this.nodeDatabase=new NodeDatabase(null);
    this.nodeDatabase=new NodeDatabase(config.getFileRelativeFromConfigDirectory("node.db"));
    this.serverSideSSLContext = new ECDSASslContext(nodeIdentity, new CheckSelfSignedNodeIdTrustManager(null));

    new NetworkApplication(this);
    ServerBootstrap b = new ServerBootstrap();
    b.group(nioWorkerGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(channelInitializer)
      .option(ChannelOption.SO_BACKLOG, 128)
      .childOption(ChannelOption.SO_KEEPALIVE, true)
//      .childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
      ;
    bindChannel = b.bind(config.getListenPort()).sync().channel();
    effectiveListeningPort=((InetSocketAddress) bindChannel.localAddress()).getPort();
    LOGGER.debug("server "+super.getNodeIdentifier()+" now listening on port {}", effectiveListeningPort);
    
    networkClient=new NetworkClient(getLocalIdentity(), getNodeDatabase());
    networkClient.setLocalListeningPort(getListeningPort());
    networkClient.useEncryption=config.encryptConnection;
  }

  private GenericFutureListener<Future<Channel>> onSslHandshakeCompletes=new GenericFutureListener<Future<Channel>>() {
    @Override
    public void operationComplete(Future<Channel> future) throws Exception {
      SocketChannel channel = (SocketChannel) future.get();
      SslHandler sslHandler=(SslHandler) channel.pipeline().get(TLS_HANDLER_NAME);
      X509Certificate[] remoteCertificate = sslHandler.engine().getSession().getPeerCertificateChain();
      NodeIdentifier remoteNodeId=new NodeIdentifier(remoteCertificate[0].getPublicKey().getEncoded());
      setRemoteNodeIdentifier(channel, remoteNodeId);
    }
  };

  ChannelInitializer<SocketChannel> channelInitializer=new ChannelInitializer<SocketChannel>() {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
      SslHandler sslHandler=serverSideSSLContext.newHandler(NetworkServer.this.configuration.encryptConnection, false);
      sslHandler.handshakeFuture().addListener(onSslHandshakeCompletes);
      ch.pipeline().addLast(TLS_HANDLER_NAME, sslHandler);

      ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));
//      ch.pipeline().addLast(new TraceHandler("Before anything"));
      ch.pipeline().addLast(new HeaderPayloadStreamDecoder());
//      ch.pipeline().addLast(new TraceHandler("Before routing"));
      ch.pipeline().addLast(applicationWorkerGroup, new RoutingHandler(NetworkServer.this));

      ch.pipeline().addLast(new HeaderAndPayloadToBytesEncoder());
      ch.pipeline().addLast(new ChunkedWriteHandler());
    }
  };

  public void stopAcceptingConnections() throws InterruptedException{
    bindChannel.close().sync();

    nioWorkerGroup.shutdownGracefully();
    applicationWorkerGroup.shutdownGracefully();
    //		nioWorkerGroup.awaitTermination(1000, TimeUnit.DAYS);
    //		applicationWorkerGroup.awaitTermination(1000, TimeUnit.DAYS);
  }


  public void addApplicationHandler(BaseApplicationMessageHandler applicationHandler) {
    this.allApplicationHandler.put(applicationHandler.getApplicationId(), applicationHandler);
  }

  public BaseApplicationMessageHandler getApplicationHandler(ApplicationIdentifier appIdReceived) {
    return allApplicationHandler.get(appIdReceived);
  }

  public void setConfig(TopLevelConfig config) {
    this.configuration=config;
  }
  public TopLevelConfig getConfig(){
    return configuration;
  }

  public int getListeningPort() {
    return effectiveListeningPort;
  }

  public InetSocketAddress getListeningAddress() {
    return (InetSocketAddress) bindChannel.localAddress();
  }

  public NodeDatabase getNodeDatabase(){
    return nodeDatabase;
  }

  public NodeIdentity getLocalIdentity() {
    return this.nodeIdentity;
  }
  
  public NodeIdentifier getRemoteNodeIdentifier(ChannelHandlerContext ctx) {
    Attribute<NodeIdentifier> nodeIdHolder = ctx.channel().attr(REMOTE_NODE_ID_ATTR);
    return nodeIdHolder.get();
  }

  public void setRemoteNodeIdentifier(Channel channel, NodeIdentifier remoteNodeId) {
    Attribute<NodeIdentifier> nodeIdHolder = channel.attr(REMOTE_NODE_ID_ATTR);
    nodeIdHolder.set(remoteNodeId);
  }



}
