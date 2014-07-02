package org.castaconcord.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

import org.castaconcord.core.BazarroApplicationIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class BazarroRoutingHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(BazarroRoutingHandler.class);
	
	private BazarroNetworkServer server;

	public BazarroRoutingHandler(BazarroNetworkServer server) {
		this.server=server;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		MDC.put("NodeId", server.thisNodeId.toString());
		HeaderAndPayload currentHeaderAndPayload = (HeaderAndPayload) msg;
		BazarroApplicationIdentifier appIdReceived = new BazarroApplicationIdentifier(currentHeaderAndPayload.header.getApplicationId().toByteArray());
		BaseBazarroApplicationMessageHandler applicationHandler=server.getApplicationHandler(appIdReceived);
		if(applicationHandler!=null){
			HeaderAndPayload response = applicationHandler.generateReponseFromQuery(ctx, currentHeaderAndPayload);
			if(response!=null){
				ctx.channel().writeAndFlush(response);
			}
		}
		else{
			LOGGER.error("Failed to find application "+appIdReceived);
		}
		ReferenceCountUtil.release(currentHeaderAndPayload.header);
		ReferenceCountUtil.release(msg);
		MDC.remove("NodeId");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
				ctx.close();
			} else if (e.state() == IdleState.WRITER_IDLE) {
				//				ctx.writeAndFlush(new PingMessage());
				LOGGER.debug("Writer idle");
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}