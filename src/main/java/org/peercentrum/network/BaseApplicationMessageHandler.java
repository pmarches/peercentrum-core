package org.peercentrum.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg;

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

	protected HeaderMsg.Builder newResponseHeaderForRequest(HeaderAndPayload receivedRequest) {
		HeaderMsg.Builder responseHeaderBuilder = PB.HeaderMsg.newBuilder();
		responseHeaderBuilder.setDestinationApplicationId(receivedRequest.header.getDestinationApplicationId());
		responseHeaderBuilder.setRequestNumber(receivedRequest.header.getRequestNumber());
		return responseHeaderBuilder;
	}
}
