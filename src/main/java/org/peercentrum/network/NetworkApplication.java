package org.peercentrum.network;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

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
  

	public NetworkApplication(NetworkServer clientOrServer) {
		super(clientOrServer);
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
        if(networkMessage.hasNodeMetaData()){
          handleReceiveNodeMetaData(ctx, networkMessage);
        }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

  private void handleReceiveNodeMetaData(ChannelHandlerContext ctx, PB.NetworkMessage networkMessage) {
    PB.NodeMetaDataMsg nodeMetaDataMsg = networkMessage.getNodeMetaData();
    if(nodeMetaDataMsg.hasNodePublicKey()){ //Do we allow anonymous clients? How to encrypt the comm if we do not have a public key?
      NodeIdentifier remoteNodeId=new NodeIdentifier(nodeMetaDataMsg.getNodePublicKey().toByteArray());
      super.setRemoteNodeIdentifier(ctx, remoteNodeId);
    }
    
    LOGGER.debug("Query has nodeMetaData {}", nodeMetaDataMsg);
    
    NodeIdentifier NodeIdentifier = new NodeIdentifier(nodeMetaDataMsg.getNodePublicKey().toByteArray());

    String externalIP;
    if(nodeMetaDataMsg.hasExternalIP()){
      externalIP=nodeMetaDataMsg.getExternalIP();
    }
    else{
      externalIP=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
    }
    InetSocketAddress ipEndpoint=new InetSocketAddress(externalIP, nodeMetaDataMsg.getExternalPort());
    server.getNodeDatabase().mapNodeIdToAddress(NodeIdentifier, ipEndpoint);
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
