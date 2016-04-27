package org.peercentrum.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.security.cert.X509Certificate;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ServerMain;
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

public class NetworkServer { //TODO implement AutoClosable
  private static final String TLS_HANDLER_NAME = "tls";
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkServer.class);
  protected static final AttributeKey<NodeIdentifier> REMOTE_NODE_ID_ATTR = AttributeKey.valueOf("REMOTE_NODE_ID");

  DefaultEventExecutorGroup applicationWorkerGroup = new DefaultEventExecutorGroup(2);
  NioEventLoopGroup nioWorkerGroup = new NioEventLoopGroup();
  Channel bindChannel;
  protected int effectiveListeningPort;
  protected ECDSASslContext serverSideSSLContext;
  protected ServerMain serverMain;

  public NetworkServer(ServerMain serverMain) throws Exception {
    this.serverMain=serverMain;
    this.serverSideSSLContext = new ECDSASslContext(serverMain.getLocalIdentity(), new CheckSelfSignedNodeIdTrustManager(null));

    ServerBootstrap b = new ServerBootstrap();
    b.group(nioWorkerGroup)
      .channel(NioServerSocketChannel.class)
      .childHandler(channelInitializer)
      .option(ChannelOption.SO_BACKLOG, 128)
      .childOption(ChannelOption.SO_KEEPALIVE, true)
//      .childOption(ChannelOption.ALLOW_HALF_CLOSURE, true)
      ;
    bindChannel = b.bind(serverMain.getConfig().getListenPort()).sync().channel();
    effectiveListeningPort=((InetSocketAddress) bindChannel.localAddress()).getPort();
    LOGGER.debug("networkServer "+serverMain.getLocalIdentity().getIdentifier()+" now listening on port {}", effectiveListeningPort);
    
    if(serverMain.getConfig().getEnableNAT()){
      enableNATInboundConnections();
    }
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
      SslHandler sslHandler=serverSideSSLContext.newHandler(serverMain.getConfig().encryptConnection, false);
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

  protected void enableNATInboundConnections() throws Exception {
    GatewayDiscover discover = new GatewayDiscover();
    LOGGER.debug("Looking for gateway devices");
    discover.discover();
    final GatewayDevice natDevice = discover.getValidGateway();
    if (null != natDevice) {
      LOGGER.debug("Gateway device found {} {} ", natDevice.getModelName(), natDevice.getModelDescription());
    } else {
      LOGGER.debug("No valid gateway device found, doing nothing.");
      return;
    }

//    String externalIPAddress = natDevice.getExternalIPAddress();
//    LOGGER.debug("Our external address is {}", externalIPAddress);
//    server1.setListeningAddress(externalIPAddress);

    final int localPortToMap=getListeningPort();
    LOGGER.debug("Querying device to see if a mapping for port {} already exists", localPortToMap);

    PortMappingEntry portMapping = new PortMappingEntry();
    if (natDevice.getSpecificPortMappingEntry(localPortToMap, "TCP", portMapping)) {
      LOGGER.error("Port {} was already mapped by {}. Aborting...", localPortToMap, portMapping.getPortMappingDescription()); //TODO Maybe we should retry with another port number. Do not forget to update the local gossip info with the new port
    } else {
      LOGGER.debug("External port {} is available, sending mapping request", localPortToMap);
      InetAddress localAddress = natDevice.getLocalAddress();
      if (natDevice.addPortMapping(localPortToMap, localPortToMap, localAddress.getHostAddress(), "TCP", getClass().getName())) {
        LOGGER.info("Port mapping successfull");
        Runtime.getRuntime().addShutdownHook(new Thread(){
          public void run() {
            try {
              LOGGER.debug("deleting port mapping {}", localPortToMap);
              natDevice.deletePortMapping(localPortToMap, "TCP");
            } catch (Exception e) {
              e.printStackTrace();
            }
          };
        });
      } else {
        LOGGER.error("Port mapping failed");
      }
    }
  }

  public int getListeningPort() {
    return effectiveListeningPort;
  }

  public InetSocketAddress getListeningAddress() {
    return (InetSocketAddress) bindChannel.localAddress();
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
