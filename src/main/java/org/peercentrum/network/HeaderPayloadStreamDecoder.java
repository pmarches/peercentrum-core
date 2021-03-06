package org.peercentrum.network;
import java.util.List;

import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;


public class HeaderPayloadStreamDecoder extends ByteToMessageDecoder {
  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderPayloadStreamDecoder.class);
  PB.HeaderMsg pendingHeader;
  int nbBytesLeftToStream;
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if(pendingHeader==null){
      pendingHeader=ProtobufByteBufCodec.decodeWithLengthPrefix(in, PB.HeaderMsg.class);
      if(pendingHeader==null){
        return;
      }
      if(pendingHeader.hasApplicationSpecificStreamLength()){
        nbBytesLeftToStream=pendingHeader.getApplicationSpecificStreamLength();
      }
      else{
        nbBytesLeftToStream=0;
      }
    }

    if(pendingHeader.hasApplicationSpecificBlockLength()){
      if(pendingHeader.hasApplicationSpecificStreamLength()){
        throw new Exception("Cannot include a stream with a block");
      }
      int appSpecificBlockLength=pendingHeader.getApplicationSpecificBlockLength();
      if(in.readableBytes()<appSpecificBlockLength){
        return;
      }
      ByteBuf applicationBlock=in.readBytes(appSpecificBlockLength);
      HeaderMsg headerReadyToBePropagated = pendingHeader;
      pendingHeader=null;
      HeaderAndPayload headerAndPayload = new HeaderAndPayload(headerReadyToBePropagated, applicationBlock);
      ctx.fireChannelRead(headerAndPayload);
    }

    int nbByteToStreamInThisPass=Math.min(in.readableBytes(), nbBytesLeftToStream);
    if(nbByteToStreamInThisPass==0){
      return;
    }
    nbBytesLeftToStream-=nbByteToStreamInThisPass;

    ByteBuf bytesToStream; //Premature optimization is the root of all evils. But what the heck, it is so much fun!
    if(nbBytesLeftToStream==0 && nbByteToStreamInThisPass!=in.readableBytes()){
      bytesToStream=in.readBytes(nbByteToStreamInThisPass); //Split the byteBuf
    }
    else{
      bytesToStream=in;
    }
    if(bytesToStream!=Unpooled.EMPTY_BUFFER){
      BaseStreamHandler streamHandler=BaseStreamHandler.getCurrentStreamHandlerFromContext(ctx);
      if(streamHandler==null){
        LOGGER.warn("No stream handler has been set for message "+pendingHeader+" so stream will be discarded");
      }
      else{
        streamHandler.onStreamBytes(bytesToStream);
      }
      bytesToStream.readerIndex(bytesToStream.writerIndex()); //Ensure we have consumed all of the stream bytes
    }

    if(nbBytesLeftToStream==0){
      pendingHeader=null;

      BaseStreamHandler streamHandler=BaseStreamHandler.getCurrentStreamHandlerFromContext(ctx);
      if(streamHandler==null){
        LOGGER.warn("No stream handler has been set for message "+pendingHeader);
      }
      else{
        streamHandler.onEndStream(ctx);
      }

      return;
    }
  }



}