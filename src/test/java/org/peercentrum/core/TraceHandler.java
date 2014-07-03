package org.peercentrum.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class TraceHandler extends ChannelDuplexHandler {
		private String message;

		public TraceHandler(String message) {
			this.message=message;
		}
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("READ "+message+". "+msg);
//			System.out.println("TRACE pipeline is now "+ctx.pipeline().names());
			super.channelRead(ctx, msg);
		}
		
		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			System.out.println("USEREVENT "+message+". "+evt);
			super.userEventTriggered(ctx, evt);
		}
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			System.out.println("WRITE "+message+". "+msg);
			super.write(ctx, msg, promise);
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.err.println("Exception caugt");
			cause.printStackTrace();
			ctx.close();
			System.exit(1);
		}
		
		@Override
		public String toString() {
			return super.toString()+message;
		}
	}