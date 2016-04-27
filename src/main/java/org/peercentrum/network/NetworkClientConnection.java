package org.peercentrum.network;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.NodeIPEndpoint;
import org.peercentrum.core.PB;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

public class NetworkClientConnection implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClientConnection.class);

  NioEventLoopGroup workerGroup = new NioEventLoopGroup(); //TODO Pass-in as argument
  ChannelFuture socketChannelFuture;
  AtomicInteger requestCounter = new AtomicInteger();
  ConcurrentHashMap<Integer, DefaultPromise<ByteBuf>> pendingRequests = new  ConcurrentHashMap<>();
  NodeIdentifier localNodeId;
  NodeIPEndpoint remoteEndpoint;
  ECDSASslContext sslCtx;
  InetSocketAddress serverEndpoint;
  private boolean useEncryption;

  public NetworkClientConnection(NetworkClient networkClient, NodeIPEndpoint remoteEndpoint, final int localListeningPort) throws Exception {
    this.remoteEndpoint=remoteEndpoint;
    this.sslCtx = new ECDSASslContext(networkClient.nodeIdentity, new CheckSelfSignedNodeIdTrustManager(remoteEndpoint.getNodeId()));
    this.useEncryption=networkClient.useEncryption;
    
    Bootstrap b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioSocketChannel.class);
    b.option(ChannelOption.SO_KEEPALIVE, true);
    b.handler(channelInitializer);

    socketChannelFuture = b.connect(remoteEndpoint.getAddress());
    socketChannelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override public void operationComplete(Future<? super Void> future) throws Exception {
        sendLocalNodeMetaData(localListeningPort);
      }
    });
  }

  ChannelInitializer<SocketChannel> channelInitializer=new ChannelInitializer<SocketChannel>(){
    protected void initChannel(SocketChannel ch) throws Exception {
      SslHandler sslHandler=sslCtx.newHandler(useEncryption, true);
      ch.pipeline().addLast(sslHandler);
      //    		ch.pipeline().addLast(new TraceHandler("Before client bytes decoder"));
      ch.pipeline().addLast(new HeaderPayloadStreamDecoder());
      //    		ch.pipeline().addLast(new TraceHandler("Before client message handler"));
      ch.pipeline().addLast(headerAndPayloadHandler);

      ch.pipeline().addLast(new HeaderAndPayloadToBytesEncoder());
    };
  };

  ChannelInboundHandlerAdapter headerAndPayloadHandler = new ChannelInboundHandlerAdapter(){
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      HeaderAndPayload headerAndPayloadRead = (HeaderAndPayload) msg;
      int requestNumber=headerAndPayloadRead.header.getRequestNumber();
      DefaultPromise<ByteBuf> responseFuture=pendingRequests.remove(requestNumber);
      if(responseFuture==null){
        LOGGER.error("No pending request "+requestNumber);
        return;
      }
      responseFuture.setSuccess(headerAndPayloadRead.payload);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
    };
  };

  protected void sendLocalNodeMetaData(int localListeningPort){
    PB.NodeMetaDataMsg.Builder nodeMetaDataBuilder=PB.NodeMetaDataMsg.newBuilder();
    nodeMetaDataBuilder.setUserAgent(getClass().getName());
    //        nodeMetaDataBuilder.setExternalIP(effectiveListeningPort.getAddress().getHostName());
    if(localListeningPort!=0){
      nodeMetaDataBuilder.setExternalPort(localListeningPort);          
    }
    PB.NetworkMessage.Builder networkMsg=PB.NetworkMessage.newBuilder();
    networkMsg.setNodeMetaData(nodeMetaDataBuilder);
    Future<PB.NetworkMessage> remoteNodeMetaDataResponseF = sendRequestMsg(NetworkApplication.NETWORK_APPID, networkMsg.build());
  }

  public Future<ByteBuf> sendRequestBytes(ApplicationIdentifier destinationApp, ByteBuf applicationSpecificBytesToSend) {
    return sendRequestBytes(destinationApp, applicationSpecificBytesToSend, true);
  }

  public Future<ByteBuf> sendRequestBytes(ApplicationIdentifier destinationApp, ByteBuf applicationSpecificBytesToSend, boolean expectResponse) {
    PB.HeaderMsg.Builder headerBuilder=PB.HeaderMsg.newBuilder();
    headerBuilder.setDestinationApplicationId(ByteString.copyFrom(destinationApp.getBytes()));

    int thisRequestNumber=requestCounter.incrementAndGet();
    headerBuilder.setRequestNumber(thisRequestNumber);
    DefaultPromise<ByteBuf> responseFuture = new DefaultPromise<ByteBuf>(socketChannelFuture.channel().eventLoop());
    if(expectResponse){
      pendingRequests.put(thisRequestNumber, responseFuture);
    }
    else{
      responseFuture.setSuccess(Unpooled.EMPTY_BUFFER);
    }

    HeaderAndPayload headerAndPayload = new HeaderAndPayload(headerBuilder, applicationSpecificBytesToSend);
    socketChannelFuture.syncUninterruptibly(); //wait for connection to be up //FIXME Is the Sync right???
    socketChannelFuture.channel().writeAndFlush(headerAndPayload);
    socketChannelFuture.channel().read();
    return responseFuture;
  }

  @SuppressWarnings("unchecked")
  public <T extends MessageLite> Future<T> sendRequestMsg(ApplicationIdentifier destinationAppId, final T protobufRequest) {
    return (Future<T>) sendRequestMsg(destinationAppId, protobufRequest, protobufRequest.getClass());
  }

  public <T extends MessageLite> Future<T> sendRequestMsg(ApplicationIdentifier applicationId, MessageLite appSpecificRequestMsg, final Class<T> appSpecificResponseClass) {
    ByteBuf appSpecificProtobufBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(appSpecificRequestMsg);
    Future<ByteBuf> responseBytesFuture = sendRequestBytes(applicationId, appSpecificProtobufBytes);
    //FIXME should we release() something?

    //FIXME Hum, is that the proper thread to do the decoding?
    final DefaultPromise<T> responseFuture = new DefaultPromise<T>(GlobalEventExecutor.INSTANCE);
    responseBytesFuture.addListener(new GenericFutureListener<Future<? super ByteBuf>>() {
      @Override
      public void operationComplete(Future<? super ByteBuf> future) throws Exception {
        if(future.isSuccess()==false){
          responseFuture.setFailure(future.cause());
          return;
        }
        T decodedAppSpecificResponse=(T) ProtobufByteBufCodec.decodeNoLengthPrefix((ByteBuf) future.get(), appSpecificResponseClass);
        responseFuture.setSuccess(decodedAppSpecificResponse);
      }
    });
    return responseFuture;
  }

  @Override
  public void close() {
    //    while(false==socketChannelFuture.channel().isWritable()){
    //      try {
    //        Thread.sleep(100);
    //      } catch (InterruptedException e) {
    //        e.printStackTrace();
    //      }
    //    }
    if(pendingRequests.isEmpty()==false){
      LOGGER.error("Closing connection while pending requests still exists: "+pendingRequests.keySet());
    }
    if(socketChannelFuture.channel().isOpen()){
      sendRequestBytes(NetworkApplication.NETWORK_APPID, NetworkApplication.getCloseMessageBytes());
    }
    socketChannelFuture.channel().close().syncUninterruptibly();
    workerGroup.shutdownGracefully(); //FIXME Not our responsability if passed in as argument..
    LOGGER.debug("Connection closed");
  }

  public void ping() {
    Future<ByteBuf> pingResponseFuture = sendRequestBytes(NetworkApplication.NETWORK_APPID, NetworkApplication.pingMessageBytes);
    pingResponseFuture.awaitUninterruptibly(); //TODO Add timeout
  }

  public NodeIdentifier getRemoteNodeId(){
    return remoteEndpoint.getNodeId();
  }
}
