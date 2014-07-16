package org.peercentrum.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.net.InetSocketAddress;
import java.util.Hashtable;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkServer { //TODO implement AutoClosable
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkServer.class);

  DefaultEventExecutorGroup applicationWorkerGroup = new DefaultEventExecutorGroup(2);
  NioEventLoopGroup nioWorkerGroup = new NioEventLoopGroup();
  Channel bindChannel;
  TopLevelConfig configuration;
  Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler> allApplicationHandler=new Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler>();
  protected int effectiveListeningPort;
  protected NodeDatabase nodeDatabase;
  protected NodeIdentifier thisNodeId;

  ChannelInitializer<SocketChannel> channelInitializer=new ChannelInitializer<SocketChannel>() {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
      ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(60, 30, 0));
//      ch.pipeline().addLast(new TraceHandler("Before anything"));
      ch.pipeline().addLast(new HeaderPayloadStreamDecoder());
//      ch.pipeline().addLast(new TraceHandler("Before routing"));
      ch.pipeline().addLast(applicationWorkerGroup, new RoutingHandler(NetworkServer.this));

      ch.pipeline().addLast(new HeaderAndPayloadToBytesEncoder());
      ch.pipeline().addLast(new ChunkedWriteHandler());
    }
  };

  public NetworkServer(TopLevelConfig config) throws InterruptedException {
    this.configuration=config;
    this.thisNodeId=new NodeIdentifier(config.getNodeIdentifier().getBytes());
    this.nodeDatabase=new NodeDatabase(config.getFile(config.getNodeDatabasePath()));

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
    LOGGER.debug("network server1 now listening on port {}", effectiveListeningPort);
  }

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

  public NodeDatabase getNodeDatabase(){
    return nodeDatabase;
  }

  public NodeIdentifier getLocalNodeId(){
    return thisNodeId;
  }
}
