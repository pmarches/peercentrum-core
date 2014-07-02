package org.castaconcord.network;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import org.castaconcord.core.ProtocolBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HeaderPayloadStreamDecoder extends ByteToMessageDecoder {
	private static final Logger LOGGER = LoggerFactory.getLogger(HeaderPayloadStreamDecoder.class);
	HeaderAndPayload pendingHeaderAndPayload;
	ProtocolBuffer.BazarroHeaderMessage pendingHeader;
	ByteBuf pendingApplicationBlock;
	int nbBytesLeftToStream;
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if(pendingHeader==null){
			pendingHeader=ProtobufByteBufCodec.decodeWithLengthPrefix(in, ProtocolBuffer.BazarroHeaderMessage.class);
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
			if(pendingApplicationBlock==null){
				if(in.readableBytes()<pendingHeader.getApplicationSpecificBlockLength()){
					return;
				}
				pendingApplicationBlock=in.readBytes(pendingHeader.getApplicationSpecificBlockLength());
			}
		}

		if(pendingHeaderAndPayload==null){ //At this point the header and the blockPayload are known
			pendingHeaderAndPayload = new HeaderAndPayload(pendingHeader, pendingApplicationBlock);
			ctx.fireChannelRead(pendingHeaderAndPayload);
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
			pendingHeaderAndPayload=null;
			pendingHeader=null;
			pendingApplicationBlock=null;

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