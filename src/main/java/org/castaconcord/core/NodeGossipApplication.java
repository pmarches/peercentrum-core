package org.castaconcord.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.castaconcord.core.ProtocolBuffer.GossipMessage;
import org.castaconcord.core.ProtocolBuffer.GossipReplyMorePeers;
import org.castaconcord.core.ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint;
import org.castaconcord.core.ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint.IPEndpoint;
import org.castaconcord.core.ProtocolBuffer.HeaderMessage.Builder;
import org.castaconcord.core.ProtocolBuffer.SenderInformationMsg;
import org.castaconcord.network.BaseApplicationMessageHandler;
import org.castaconcord.network.NetworkClient;
import org.castaconcord.network.NetworkClientConnection;
import org.castaconcord.network.NetworkServer;
import org.castaconcord.network.HeaderAndPayload;
import org.castaconcord.network.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * FIXME The application should be an aggregate of the client and the server classes. Need to add
 * a getClient() and getServer() methods. All the common stuff can go in the application ?
 */
public class NodeGossipApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipApplication.class);
	
	public static final ApplicationIdentifier GOSSIP_ID=new ApplicationIdentifier((NodeGossipApplication.class.getSimpleName()+"_APP_ID").getBytes()); 
	
	public NodeGossipApplication(NetworkServer server) {
		super(server);
		if(clientOrServer.getNodeDatabase().size()==0){
			try {
				LOGGER.info("Node database is empty, will try to bootstrap, if possible");
				bootstrapGossiping();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			NodeDatabase nodeDb = clientOrServer.getNodeDatabase();
			if(receivedMessage.header.hasSenderInfo()){
				SenderInformationMsg senderInfo = receivedMessage.header.getSenderInfo();
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
			GossipMessage gossipRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, GossipMessage.class);
			//TODO Check the application filter
			if(gossipRequest.hasRequestMorePeers()==false){
				LOGGER.error("Invalid gossip request");
				return null;
			}
			Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			GossipMessage.Builder gossipPayloadBuilder=GossipMessage.newBuilder();
			GossipReplyMorePeers.Builder gossipReplyBuilder=GossipReplyMorePeers.newBuilder();
			for(NodeInformation oneNodeInfo:nodeDb.getAllNodeInformation(50)){
				PeerEndpoint.Builder onePeerBuilder = PeerEndpoint.newBuilder();
				onePeerBuilder.setIdentity(ByteString.copyFrom(oneNodeInfo.publicKey.getBytes()));
				onePeerBuilder.setIpEndpoint(IPEndpoint.newBuilder().setIpaddress(oneNodeInfo.nodeSocketAddress.getHostString()).setPort(oneNodeInfo.nodeSocketAddress.getPort()));
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

	/**
	 * Typically called by the client
	 * @param serverId
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public void exchangeNodes(final NetworkClient client, NodeIdentifier peerIdToExchangeWith) throws InterruptedException, ExecutionException {
		ProtocolBuffer.GossipMessage.Builder gossipReqBuilder=ProtocolBuffer.GossipMessage.newBuilder();
		gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.GossipRequestMorePeers.getDefaultInstance());
		
		Future<GossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, GOSSIP_ID, gossipReqBuilder.build());
		integrateGossipResponse(responseFuture.get(), clientOrServer.getNodeDatabase());
	}

	public void bootstrapGossiping() throws Exception {
		Config topConfig=clientOrServer.getConfig();
		NodeGossipConfig gossipConfig=(NodeGossipConfig) topConfig.getAppConfig(NodeGossipConfig.class);
		if(gossipConfig.getBootstrapEndpoint()!=null){
			LOGGER.info("Starting bootstrap with {}", gossipConfig.getBootstrapEndpoint());
			String[] addressAndPort=gossipConfig.getBootstrapEndpoint().split(":");
			if(addressAndPort==null || addressAndPort.length!=2){
				LOGGER.error("bootstrap entry has not been recognized {}", gossipConfig.getBootstrapEndpoint());
				return;
			}
			InetSocketAddress bootstrapAddress=new InetSocketAddress(addressAndPort[0], Integer.parseInt(addressAndPort[1]));
			LOGGER.debug("bootstrap is {}", bootstrapAddress);

			NetworkClientConnection newConnection = new NetworkClientConnection(bootstrapAddress);
			newConnection.setLocalNodeInfo(clientOrServer.getLocalNodeId(), clientOrServer.getListeningPort());
			
			ProtocolBuffer.GossipMessage.Builder gossipReqBuilder=ProtocolBuffer.GossipMessage.newBuilder();
			gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.GossipRequestMorePeers.getDefaultInstance());
			GossipMessage response=newConnection.sendRequestMsg(getApplicationId(), gossipReqBuilder.build()).get();
			newConnection.close();

			integrateGossipResponse(response, clientOrServer.getNodeDatabase());
			LOGGER.debug("After bootstrap we have {} peers to try.", clientOrServer.getNodeDatabase().size());
		}
	}

	protected void integrateGossipResponse(GossipMessage response, NodeDatabase nodeDb) {
		if(response.hasReply()){
			GossipReplyMorePeers gossipReply = response.getReply();
			for(PeerEndpoint peer : gossipReply.getPeersList()){
				NodeIdentifier NodeIdentifier = new NodeIdentifier(peer.getIdentity().toByteArray());
				InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getIpEndpoint().getIpaddress(), peer.getIpEndpoint().getPort());
				nodeDb.mapNodeIdToAddress(NodeIdentifier, ipEndpoint);
			}
		}
	}
}
