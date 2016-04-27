package org.peercentrum.network;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg;
import org.peercentrum.core.ServerMain;

import io.netty.channel.ChannelHandlerContext;

public abstract class BaseApplicationMessageHandler {
	protected ServerMain serverMain;
	
	public BaseApplicationMessageHandler(ServerMain serverMain){
		if(serverMain!=null){
			this.serverMain=serverMain;
			serverMain.addApplicationHandler(this);
		}
	}
	
	public abstract ApplicationIdentifier getApplicationId();
	public abstract HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage);

	protected HeaderMsg.Builder newResponseHeaderForRequest(HeaderAndPayload receivedRequest) {
		HeaderMsg.Builder responseHeaderBuilder = PB.HeaderMsg.newBuilder();
		responseHeaderBuilder.setDestinationApplicationId(receivedRequest.header.getDestinationApplicationId());
		responseHeaderBuilder.setRequestNumber(receivedRequest.header.getRequestNumber());
		return responseHeaderBuilder;
	}
}
