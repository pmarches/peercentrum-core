package org.castaconcord.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import org.castaconcord.core.BazarroApplicationIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.BazarroNetworkMessage;
import org.castaconcord.core.ProtocolBuffer.BazarroNetworkMessage.NetworkOperation;

public class BazarroNetworkApplication extends BaseBazarroApplicationMessageHandler {

	public BazarroNetworkApplication(BazarroNetworkServer clientOrServer) {
		super(clientOrServer);
	}

	public static final BazarroApplicationIdentifier BNETWORK_APPID=new BazarroApplicationIdentifier(BazarroNetworkApplication.class.getName().getBytes());

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
			BazarroNetworkMessage networkMessage = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, BazarroNetworkMessage.class);
			if(networkMessage.hasOperation()){
				if(networkMessage.getOperation()==BazarroNetworkMessage.NetworkOperation.CLOSE_CONNECTION){
//					System.out.println("Got a close request from the client");
					ctx.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public BazarroApplicationIdentifier getApplicationId() {
		return BNETWORK_APPID;
	}
	
	public static ByteBuf getCloseMessageBytes(){
		ProtocolBuffer.BazarroNetworkMessage closeMsg=ProtocolBuffer.BazarroNetworkMessage.newBuilder()
				.setOperation(NetworkOperation.CLOSE_CONNECTION).build();
		return Unpooled.wrappedBuffer(closeMsg.toByteArray());
	}
}
