package org.castaconcord.network;

import io.netty.channel.ChannelHandlerContext;

import org.castaconcord.core.BazarroApplicationIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.BazarroHeaderMessage;

public abstract class BaseBazarroApplicationMessageHandler {
	protected BazarroNetworkServer clientOrServer;
	
	public BaseBazarroApplicationMessageHandler(BazarroNetworkServer clientOrServer){
		if(clientOrServer!=null){
			this.clientOrServer=clientOrServer;
			clientOrServer.addApplicationHandler(this);
		}
	}
	
	public abstract BazarroApplicationIdentifier getApplicationId();
	public abstract HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage);

	protected BazarroHeaderMessage.Builder newResponseHeaderForRequest(HeaderAndPayload receivedRequest) {
		BazarroHeaderMessage.Builder responseHeaderBuilder = ProtocolBuffer.BazarroHeaderMessage.newBuilder();
		responseHeaderBuilder.setApplicationId(receivedRequest.header.getApplicationId());
		responseHeaderBuilder.setRequestNumber(receivedRequest.header.getRequestNumber());
		return responseHeaderBuilder;
	}
}
