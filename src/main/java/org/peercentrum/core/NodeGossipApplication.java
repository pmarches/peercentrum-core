package org.peercentrum.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

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
  
	public NodeGossipApplication(NetworkServer server) throws Exception {
		super(server);
		client=new NodeGossipClient(server.networkClient);
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
	  if(gossipConfig!=null){
	    client.bootstrapGossiping(gossipConfig.getBootstrapEndpoint());
	  }
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
			
			if(receivedMessage.header.hasApplicationSpecificBlockLength()==false){
				LOGGER.error("Request is missing the application block payload");
				return null;
			}
			PB.GossipMessage gossipRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, PB.GossipMessage.class);
			//TODO Check the application filter
			if(gossipRequest.hasRequestMorePeers()==false){
				LOGGER.error("Invalid gossip request");
				return null;
			}
			PB.HeaderMsg.Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			PB.GossipMessage.Builder gossipPayloadBuilder=PB.GossipMessage.newBuilder();
			PB.GossipReplyMorePeers.Builder gossipReplyBuilder=PB.GossipReplyMorePeers.newBuilder();
			for(NodeMetaData oneNodeInfo:nodeDb.getAllNodeInformation(50)){
			  PB.PeerEndpointMsg.Builder onePeerBuilder = PB.PeerEndpointMsg.newBuilder();
				onePeerBuilder.setIdentity(ByteString.copyFrom(oneNodeInfo.publicKey.getBytes()));
				onePeerBuilder.setIpEndpoint(PB.PeerEndpointMsg.IPEndpointMsg.newBuilder().setIpAddress(oneNodeInfo.nodeSocketAddress.getHostString()).setPort(oneNodeInfo.nodeSocketAddress.getPort()));
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
