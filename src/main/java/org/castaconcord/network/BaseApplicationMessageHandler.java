package org.castaconcord.network;

import io.netty.channel.ChannelHandlerContext;

import org.castaconcord.core.ApplicationIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.HeaderMessage;

public abstract class BaseApplicationMessageHandler {
	protected NetworkServer clientOrServer;
	
	public BaseApplicationMessageHandler(NetworkServer clientOrServer){
		if(clientOrServer!=null){
			this.clientOrServer=clientOrServer;
			clientOrServer.addApplicationHandler(this);
		}
	}
	
	public abstract ApplicationIdentifier getApplicationId();
	public abstract HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage);

	protected HeaderMessage.Builder newResponseHeaderForRequest(HeaderAndPayload receivedRequest) {
		HeaderMessage.Builder responseHeaderBuilder = ProtocolBuffer.HeaderMessage.newBuilder();
		responseHeaderBuilder.setApplicationId(receivedRequest.header.getApplicationId());
		responseHeaderBuilder.setRequestNumber(receivedRequest.header.getRequestNumber());
		return responseHeaderBuilder;
	}
}
