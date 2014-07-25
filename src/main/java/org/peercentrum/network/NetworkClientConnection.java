package org.peercentrum.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMessage;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

public class NetworkClientConnection implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClientConnection.class);

  NioEventLoopGroup workerGroup = new NioEventLoopGroup(); //TODO Pass-in as argument
  ChannelFuture socketChannelFuture;
  AtomicInteger requestCounter = new AtomicInteger();
  ConcurrentHashMap<Integer, DefaultPromise<ByteBuf>> pendingRequests = new  ConcurrentHashMap<>();
  PB.SenderInformationMsg senderInfo;

  ChannelInitializer<SocketChannel> channelInitializer=new ChannelInitializer<SocketChannel>(){
    protected void initChannel(SocketChannel ch) throws Exception {
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

  NodeIdentifier localNodeId;

  public NetworkClientConnection(NodeIdentifier localNodeId, InetSocketAddress serverAddress) {
    Bootstrap b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioSocketChannel.class);
    b.option(ChannelOption.SO_KEEPALIVE, true);
    b.handler(channelInitializer);

    socketChannelFuture = b.connect(serverAddress);
    this.localNodeId=localNodeId;
  }

  public Future<ByteBuf> sendRequestBytes(ApplicationIdentifier destinationApp, ByteBuf applicationSpecificBytesToSend) {
    HeaderMessage.Builder headerBuilder=HeaderMessage.newBuilder();
    headerBuilder.setApplicationId(ByteString.copyFrom(destinationApp.getBytes()));
    if(senderInfo!=null){
      headerBuilder.setSenderInfo(senderInfo);
    }
    if(localNodeId!=null){
      headerBuilder.setNodePublicKey(ByteString.copyFrom(this.localNodeId.getBytes()));
    }

    int thisRequestNumber=requestCounter.incrementAndGet();
    headerBuilder.setRequestNumber(thisRequestNumber);
    DefaultPromise<ByteBuf> responseFuture = new DefaultPromise<ByteBuf>(socketChannelFuture.channel().eventLoop());
    pendingRequests.put(thisRequestNumber, responseFuture);

    HeaderAndPayload headerAndPayload = new HeaderAndPayload(headerBuilder, applicationSpecificBytesToSend);
    socketChannelFuture.syncUninterruptibly(); //wait for connection to be up
    socketChannelFuture.channel().writeAndFlush(headerAndPayload).syncUninterruptibly();
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

  public void setLocalNodeInfo(NodeIdentifier localNodeId, int localListeningPort) {
    PB.SenderInformationMsg.Builder senderInfoBuilder=PB.SenderInformationMsg.newBuilder();
    senderInfoBuilder.setUserAgent(getClass().getName());
    senderInfoBuilder.setNodePublicKey(ByteString.copyFrom(localNodeId.getBytes()));
    //        senderInfoBuilder.setExternalIP(effectiveListeningPort.getAddress().getHostName());
    if(localListeningPort!=0){
      senderInfoBuilder.setExternalPort(localListeningPort);          
    }
    this.senderInfo=senderInfoBuilder.build();
  }

}
