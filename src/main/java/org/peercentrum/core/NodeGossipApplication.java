package org.peercentrum.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * FIXME The application should be an aggregate of the client and the server1 classes. Need to add
 * a getClient() and getServer() methods. All the common stuff can go in the application ?
 */
public class NodeGossipApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipApplication.class);
	public static final ApplicationIdentifier GOSSIP_ID=new ApplicationIdentifier((NodeGossipApplication.class.getSimpleName()+"_APP_ID").getBytes()); 

	NodeGossipClient client;
  
	public NodeGossipApplication(NetworkServer server) {
		super(server);
		client=new NodeGossipClient(server.getLocalNodeId(), server.getNodeDatabase());
		client.reachableListeningPort=server.getListeningPort();
		if(server.getNodeDatabase().size()==0){
			try {
				LOGGER.info("Node database is empty, will try to bootstrap, if possible");
				bootstrapGossiping();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void bootstrapGossiping() throws Exception {
	  NodeGossipConfig gossipConfig=(NodeGossipConfig) server.getConfig().getAppConfig(NodeGossipConfig.class);
	  client.bootstrapGossiping(gossipConfig.getBootstrapEndpoint());
  }

  @Override
	public ApplicationIdentifier getApplicationId() {
		return GOSSIP_ID;
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
			LOGGER.debug("generateReponseFromQuery");
			NodeDatabase nodeDb = server.getNodeDatabase();
			if(receivedMessage.header.hasSenderInfo()){
			  ProtocolBuffer.SenderInformationMsg senderInfo = receivedMessage.header.getSenderInfo();
				LOGGER.debug("Query has senderinfo {}", senderInfo);
				
				NodeIdentifier NodeIdentifier = new NodeIdentifier(senderInfo.getNodePublicKey().toByteArray());

				String externalIP;
				if(senderInfo.hasExternalIP()){
					externalIP=senderInfo.getExternalIP();
				}
				else{
					externalIP=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
				}
				InetSocketAddress ipEndpoint=new InetSocketAddress(externalIP, senderInfo.getExternalPort());
				nodeDb.mapNodeIdToAddress(NodeIdentifier, ipEndpoint);
			}
			
			if(receivedMessage.header.hasApplicationSpecificBlockLength()==false){
				LOGGER.error("Request is missing the application block payload");
				return null;
			}
			ProtocolBuffer.GossipMessage gossipRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, ProtocolBuffer.GossipMessage.class);
			//TODO Check the application filter
			if(gossipRequest.hasRequestMorePeers()==false){
				LOGGER.error("Invalid gossip request");
				return null;
			}
			ProtocolBuffer.HeaderMessage.Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			ProtocolBuffer.GossipMessage.Builder gossipPayloadBuilder=ProtocolBuffer.GossipMessage.newBuilder();
			ProtocolBuffer.GossipReplyMorePeers.Builder gossipReplyBuilder=ProtocolBuffer.GossipReplyMorePeers.newBuilder();
			for(NodeInformation oneNodeInfo:nodeDb.getAllNodeInformation(50)){
			  ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint.Builder onePeerBuilder = ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint.newBuilder();
				onePeerBuilder.setIdentity(ByteString.copyFrom(oneNodeInfo.publicKey.getBytes()));
				onePeerBuilder.setIpEndpoint(ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint.IPEndpoint.newBuilder().setIpaddress(oneNodeInfo.nodeSocketAddress.getHostString()).setPort(oneNodeInfo.nodeSocketAddress.getPort()));
				gossipReplyBuilder.addPeers(onePeerBuilder.build());
			}
			gossipPayloadBuilder.setReply(gossipReplyBuilder);
			ByteBuf payloadBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(gossipPayloadBuilder.build());
			return new HeaderAndPayload(headerResponse, payloadBytes);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
