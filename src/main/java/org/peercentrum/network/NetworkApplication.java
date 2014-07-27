package org.peercentrum.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.NetworkMessage;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkApplication extends BaseApplicationMessageHandler {
  public static final ApplicationIdentifier NETWORK_APPID=new ApplicationIdentifier(NetworkApplication.class.getName().getBytes());
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkApplication.class);
  

	public NetworkApplication(NetworkServer server) {
		super(server);
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
		  PB.NetworkMessage networkMessage = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, NetworkMessage.class);
			if(networkMessage.hasOperation()){
				if(networkMessage.getOperation()==PB.NetworkMessage.NetworkOperation.CLOSE_CONNECTION){
//					System.out.println("Got a close request from the client");
					ctx.close();
				}

        if(networkMessage.getOperation()==PB.NetworkMessage.NetworkOperation.PING){
          PB.HeaderMsg.Builder responseHeader = super.newResponseHeaderForRequest(receivedMessage);
          return new HeaderAndPayload(responseHeader, pongMessageBytes);
        }
			}
			else if(networkMessage.hasNodeMetaData()){
        PB.HeaderMsg.Builder responseHeader = super.newResponseHeaderForRequest(receivedMessage);
			  return handleReceiveNodeMetaData(ctx, responseHeader, networkMessage);
			}
			else{
			  LOGGER.warn("Unhandled network message");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

  private HeaderAndPayload handleReceiveNodeMetaData(ChannelHandlerContext ctx, PB.HeaderMsg.Builder responseHeader, PB.NetworkMessage networkMessage) {
    PB.NodeMetaDataMsg receivedNodeMetaDataMsg = networkMessage.getNodeMetaData();
    LOGGER.debug("Query has nodeMetaData {}", receivedNodeMetaDataMsg);
    if(receivedNodeMetaDataMsg.hasNodePublicKey()){ //Do we allow anonymous clients? How to encrypt the comm if we do not have a public key?
      NodeIdentifier remoteNodeId=new NodeIdentifier(receivedNodeMetaDataMsg.getNodePublicKey().toByteArray());
      super.setRemoteNodeIdentifier(ctx, remoteNodeId);

      String clientExternalIP;
      if(receivedNodeMetaDataMsg.hasExternalIP()){
        clientExternalIP=receivedNodeMetaDataMsg.getExternalIP();
      }
      else{
        clientExternalIP=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
      }
      InetSocketAddress ipEndpoint=new InetSocketAddress(clientExternalIP, receivedNodeMetaDataMsg.getExternalPort());
      server.getNodeDatabase().mapNodeIdToAddress(remoteNodeId, ipEndpoint);
    }

    PB.NetworkMessage.Builder repliedNetworkMsg=PB.NetworkMessage.newBuilder();
    PB.NodeMetaDataMsg.Builder repliedNodeMetaDataMsg=PB.NodeMetaDataMsg.newBuilder();
    repliedNodeMetaDataMsg.setNodePublicKey(server.getLocalNodeId().toByteString());
    repliedNetworkMsg.setNodeMetaData(repliedNodeMetaDataMsg);
    return new HeaderAndPayload(responseHeader, Unpooled.wrappedBuffer(repliedNetworkMsg.build().toByteArray()));
  }

	@Override
	public ApplicationIdentifier getApplicationId() {
		return NETWORK_APPID;
	}
	
	public static ByteBuf getCloseMessageBytes(){
		PB.NetworkMessage closeMsg=PB.NetworkMessage.newBuilder()
				.setOperation(PB.NetworkMessage.NetworkOperation.CLOSE_CONNECTION).build();
		return Unpooled.wrappedBuffer(closeMsg.toByteArray());
	}
	
	public static ByteBuf pingMessageBytes=Unpooled.wrappedBuffer(
	    PB.NetworkMessage.newBuilder()
      .setOperation(PB.NetworkMessage.NetworkOperation.PING).build().toByteArray()
	    );

	public static ByteBuf pongMessageBytes=Unpooled.wrappedBuffer(
      PB.NetworkMessage.newBuilder()
      .setOperation(PB.NetworkMessage.NetworkOperation.PONG).build().toByteArray()
      );
}
