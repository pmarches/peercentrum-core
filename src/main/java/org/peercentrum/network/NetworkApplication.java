package org.peercentrum.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.HeaderMessage;
import org.peercentrum.core.ProtocolBuffer.NetworkMessage;
import org.peercentrum.core.ProtocolBuffer.NetworkMessage.NetworkOperation;

public class NetworkApplication extends BaseApplicationMessageHandler {

	public NetworkApplication(NetworkServer clientOrServer) {
		super(clientOrServer);
	}

	public static final ApplicationIdentifier NETWORK_APPID=new ApplicationIdentifier(NetworkApplication.class.getName().getBytes());

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
			NetworkMessage networkMessage = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, NetworkMessage.class);
			if(networkMessage.hasOperation()){
				if(networkMessage.getOperation()==NetworkMessage.NetworkOperation.CLOSE_CONNECTION){
//					System.out.println("Got a close request from the client");
					ctx.close();
				}
        if(networkMessage.getOperation()==NetworkMessage.NetworkOperation.PING){
          HeaderMessage.Builder responseHeader = super.newResponseHeaderForRequest(receivedMessage);
          return new HeaderAndPayload(responseHeader, pongMessageBytes);
        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public ApplicationIdentifier getApplicationId() {
		return NETWORK_APPID;
	}
	
	public static ByteBuf getCloseMessageBytes(){
		ProtocolBuffer.NetworkMessage closeMsg=ProtocolBuffer.NetworkMessage.newBuilder()
				.setOperation(NetworkOperation.CLOSE_CONNECTION).build();
		return Unpooled.wrappedBuffer(closeMsg.toByteArray());
	}
	
	public static ByteBuf pingMessageBytes=Unpooled.wrappedBuffer(
	    ProtocolBuffer.NetworkMessage.newBuilder()
      .setOperation(NetworkOperation.PING).build().toByteArray()
	    );

	public static ByteBuf pongMessageBytes=Unpooled.wrappedBuffer(
      ProtocolBuffer.NetworkMessage.newBuilder()
      .setOperation(NetworkOperation.PONG).build().toByteArray()
      );
}
