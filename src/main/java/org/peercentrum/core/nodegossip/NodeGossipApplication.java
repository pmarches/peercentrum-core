package org.peercentrum.core.nodegossip;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIPEndpoint;
import org.peercentrum.core.NodeMetaData;
import org.peercentrum.core.PB;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ServerMain;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * FIXME The application should be an aggregate of the client and the server1 classes. Need to add
 * a getClient() and getServer() methods. All the common stuff can go in the application ?
 */
public class NodeGossipApplication extends BaseApplicationMessageHandler implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipApplication.class);
	public static final ApplicationIdentifier GOSSIP_ID=new ApplicationIdentifier((NodeGossipApplication.class.getSimpleName()+"_APP_ID").getBytes());
  private static final int NB_CACHED_CONNECTIONS = 10; 

	NodeGossipClient client;
  
	public NodeGossipApplication(ServerMain serverMain) throws Exception {
		super(serverMain);
		client=new NodeGossipClient(serverMain.getNetworkClient(), serverMain.getNetworkServer().getListeningPort());
	}
	
	private void bootstrapGossiping() throws Exception {
	  LOGGER.info("Contacting the bootstraping node to gather more nodes");
	  NodeGossipConfig gossipConfig=(NodeGossipConfig) serverMain.getConfig().getAppConfig(NodeGossipConfig.class);
	  if(gossipConfig==null){
	    LOGGER.warn("There exists no '"+NodeGossipConfig.class.getName()+"' configuration, let's hope we are well known..");
	  }
	  else{
	    LOGGER.debug("The gossip bootstrap endpoint looks like this '"+gossipConfig.getBootstrapEndpoint()+"'");
	    NodeIPEndpoint bootstrapEndPoint=NodeIPEndpoint.parseFromString(gossipConfig.getBootstrapEndpoint());
	    client.bootstrapGossiping(bootstrapEndPoint);
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
			NodeDatabase nodeDb = serverMain.getNodeDatabase();
			
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
				onePeerBuilder.setIdentity(ByteString.copyFrom(oneNodeInfo.nodeIdentifier.getBytes()));
				onePeerBuilder.setTlsEndpoint(PB.PeerEndpointMsg.TLSEndpointMsg.newBuilder().setIpAddress(oneNodeInfo.nodeSocketAddress.getHostString()).setPort(oneNodeInfo.nodeSocketAddress.getPort()));
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

  @Override
  public void run() {
    NetworkClient client=serverMain.getNetworkClient();
    int nbMissingConnections=NB_CACHED_CONNECTIONS-client.getNumberOfCachedConnections();
    if(nbMissingConnections>0){
      LOGGER.info("We are low on cached connections, attempting to contact "+nbMissingConnections+" other nodes");
      try {
        if(serverMain.getNodeDatabase().size()<NB_CACHED_CONNECTIONS){
          bootstrapGossiping();
        }
      } catch (Exception e) {
        LOGGER.error("Exception during bootstrap gossiping", e);
      }
      try {
        for(NodeMetaData remoteNode: client.getNodeDatabase().getAllNodeInformation(nbMissingConnections)){
          client.maybeOpenConnectionToPeer(remoteNode.nodeIdentifier);
        }
      } catch (Exception e) {
        LOGGER.error("", e);
      }
    }
  }

}
