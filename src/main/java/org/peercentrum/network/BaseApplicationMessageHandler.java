package org.peercentrum.network;

import io.netty.channel.ChannelHandlerContext;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMessage;

public abstract class BaseApplicationMessageHandler {
	protected NetworkServer server;
	
	public BaseApplicationMessageHandler(NetworkServer clientOrServer){
		if(clientOrServer!=null){
			this.server=clientOrServer;
			clientOrServer.addApplicationHandler(this);
		}
	}
	
	public abstract ApplicationIdentifier getApplicationId();
	public abstract HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage);

	protected HeaderMessage.Builder newResponseHeaderForRequest(HeaderAndPayload receivedRequest) {
		HeaderMessage.Builder responseHeaderBuilder = PB.HeaderMessage.newBuilder();
		responseHeaderBuilder.setApplicationId(receivedRequest.header.getApplicationId());
		responseHeaderBuilder.setRequestNumber(receivedRequest.header.getRequestNumber());
		return responseHeaderBuilder;
	}
}
