package org.peercentrum.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

public abstract class BaseStreamHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(BaseStreamHandler.class);
	public static final AttributeKey<BaseStreamHandler> streamHandlerKey=AttributeKey.valueOf("StreamHandler");

	public BaseStreamHandler(ChannelHandlerContext ctx) {
		BaseStreamHandler oldStreamHandler=ctx.channel().attr(BaseStreamHandler.streamHandlerKey).getAndSet(this);
		if(oldStreamHandler!=null){
			LOGGER.warn("Overwriting and old stream handler. This means StreamHandler.onEndStream was not called at the end of the previous stream.");
		}
	}
	
	public void onEndStream(ChannelHandlerContext ctx){
		ctx.channel().attr(BaseStreamHandler.streamHandlerKey).remove();
	}
	
	public abstract void onStreamBytes(ByteBuf bytesToStream);
	
	public static BaseStreamHandler getCurrentStreamHandlerFromContext(ChannelHandlerContext ctx) {
		return ctx.channel().attr(BaseStreamHandler.streamHandlerKey).get();
	}
}