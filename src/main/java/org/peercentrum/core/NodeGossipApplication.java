package org.peercentrum.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.peercentrum.network.NetworkServer;
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
		
		Future<ProtocolBuffer.GossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, GOSSIP_ID, gossipReqBuilder.build());
		integrateGossipResponse(responseFuture.get(), clientOrServer.getNodeDatabase());
	}

	public void bootstrapGossiping() throws Exception {
		TopLevelConfig topConfig=clientOrServer.getConfig();
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
			ProtocolBuffer.GossipMessage response=newConnection.sendRequestMsg(getApplicationId(), gossipReqBuilder.build()).get();
			newConnection.close();

			integrateGossipResponse(response, clientOrServer.getNodeDatabase());
			LOGGER.debug("After bootstrap we have {} peers to try.", clientOrServer.getNodeDatabase().size());
		}
	}

	protected void integrateGossipResponse(ProtocolBuffer.GossipMessage response, NodeDatabase nodeDb) {
		if(response.hasReply()){
		  ProtocolBuffer.GossipReplyMorePeers gossipReply = response.getReply();
			for(ProtocolBuffer.GossipReplyMorePeers.PeerEndpoint peer : gossipReply.getPeersList()){
				NodeIdentifier NodeIdentifier = new NodeIdentifier(peer.getIdentity().toByteArray());
				InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getIpEndpoint().getIpaddress(), peer.getIpEndpoint().getPort());
				nodeDb.mapNodeIdToAddress(NodeIdentifier, ipEndpoint);
			}
		}
	}
}
