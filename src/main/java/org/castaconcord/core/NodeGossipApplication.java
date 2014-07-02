package org.castaconcord.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

import org.castaconcord.core.ProtocolBuffer.BazarroGossipMessage;
import org.castaconcord.core.ProtocolBuffer.BazarroGossipReplyMorePeers;
import org.castaconcord.core.ProtocolBuffer.BazarroGossipReplyMorePeers.PeerEndpoint;
import org.castaconcord.core.ProtocolBuffer.BazarroGossipReplyMorePeers.PeerEndpoint.IPEndpoint;
import org.castaconcord.core.ProtocolBuffer.BazarroHeaderMessage.Builder;
import org.castaconcord.core.ProtocolBuffer.SenderInformationMsg;
import org.castaconcord.network.BaseBazarroApplicationMessageHandler;
import org.castaconcord.network.BazarroNetworkClient;
import org.castaconcord.network.BazarroNetworkClientConnection;
import org.castaconcord.network.BazarroNetworkServer;
import org.castaconcord.network.HeaderAndPayload;
import org.castaconcord.network.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * FIXME The application should be an aggregate of the client and the server classes. Need to add
 * a getClient() and getServer() methods. All the common stuff can go in the application ?
 */
public class NodeGossipApplication extends BaseBazarroApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeGossipApplication.class);
	
	public static final BazarroApplicationIdentifier GOSSIP_ID=new BazarroApplicationIdentifier((NodeGossipApplication.class.getSimpleName()+"_APP_ID").getBytes()); 
	
	public NodeGossipApplication(BazarroNetworkServer server) {
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
	public BazarroApplicationIdentifier getApplicationId() {
		return GOSSIP_ID;
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
			LOGGER.debug("generateReponseFromQuery");
			BazarroNodeDatabase nodeDb = clientOrServer.getNodeDatabase();
			if(receivedMessage.header.hasSenderInfo()){
				SenderInformationMsg senderInfo = receivedMessage.header.getSenderInfo();
				LOGGER.debug("Query has senderinfo {}", senderInfo);
				
				BazarroNodeIdentifier bazarroNodeIdentifier = new BazarroNodeIdentifier(senderInfo.getNodePublicKey().toByteArray());

				String externalIP;
				if(senderInfo.hasExternalIP()){
					externalIP=senderInfo.getExternalIP();
				}
				else{
					externalIP=((InetSocketAddress)ctx.channel().remoteAddress()).getHostName();
				}
				InetSocketAddress ipEndpoint=new InetSocketAddress(externalIP, senderInfo.getExternalPort());
				nodeDb.mapNodeIdToAddress(bazarroNodeIdentifier, ipEndpoint);
			}
			
			if(receivedMessage.header.hasApplicationSpecificBlockLength()==false){
				LOGGER.error("Request is missing the application block payload");
				return null;
			}
			BazarroGossipMessage gossipRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, BazarroGossipMessage.class);
			//TODO Check the application filter
			if(gossipRequest.hasRequestMorePeers()==false){
				LOGGER.error("Invalid gossip request");
				return null;
			}
			Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			BazarroGossipMessage.Builder gossipPayloadBuilder=BazarroGossipMessage.newBuilder();
			BazarroGossipReplyMorePeers.Builder gossipReplyBuilder=BazarroGossipReplyMorePeers.newBuilder();
			for(BazarroNodeInformation oneNodeInfo:nodeDb.getAllNodeInformation(50)){
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
	public void exchangeNodes(final BazarroNetworkClient client, BazarroNodeIdentifier peerIdToExchangeWith) throws InterruptedException, ExecutionException {
		ProtocolBuffer.BazarroGossipMessage.Builder gossipReqBuilder=ProtocolBuffer.BazarroGossipMessage.newBuilder();
		gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.BazarroGossipRequestMorePeers.getDefaultInstance());
		
		Future<BazarroGossipMessage> responseFuture = client.sendRequest(peerIdToExchangeWith, GOSSIP_ID, gossipReqBuilder.build());
		integrateGossipResponse(responseFuture.get(), clientOrServer.getNodeDatabase());
	}

	public void bootstrapGossiping() throws Exception {
		BazarroConfig topConfig=clientOrServer.getConfig();
		BazarroNodeGossipConfig gossipConfig=(BazarroNodeGossipConfig) topConfig.getAppConfig(BazarroNodeGossipConfig.class);
		if(gossipConfig.getBootstrapEndpoint()!=null){
			LOGGER.info("Starting bootstrap with {}", gossipConfig.getBootstrapEndpoint());
			String[] addressAndPort=gossipConfig.getBootstrapEndpoint().split(":");
			if(addressAndPort==null || addressAndPort.length!=2){
				LOGGER.error("bootstrap entry has not been recognized {}", gossipConfig.getBootstrapEndpoint());
				return;
			}
			InetSocketAddress bootstrapAddress=new InetSocketAddress(addressAndPort[0], Integer.parseInt(addressAndPort[1]));
			LOGGER.debug("bootstrap is {}", bootstrapAddress);

			BazarroNetworkClientConnection newConnection = new BazarroNetworkClientConnection(bootstrapAddress);
			newConnection.setLocalNodeInfo(clientOrServer.getLocalNodeId(), clientOrServer.getListeningPort());
			
			ProtocolBuffer.BazarroGossipMessage.Builder gossipReqBuilder=ProtocolBuffer.BazarroGossipMessage.newBuilder();
			gossipReqBuilder.setRequestMorePeers(ProtocolBuffer.BazarroGossipRequestMorePeers.getDefaultInstance());
			BazarroGossipMessage response=newConnection.sendRequestMsg(getApplicationId(), gossipReqBuilder.build()).get();
			newConnection.close();

			integrateGossipResponse(response, clientOrServer.getNodeDatabase());
			LOGGER.debug("After bootstrap we have {} peers to try.", clientOrServer.getNodeDatabase().size());
		}
	}

	protected void integrateGossipResponse(BazarroGossipMessage response, BazarroNodeDatabase nodeDb) {
		if(response.hasReply()){
			BazarroGossipReplyMorePeers gossipReply = response.getReply();
			for(PeerEndpoint peer : gossipReply.getPeersList()){
				BazarroNodeIdentifier bazarroNodeIdentifier = new BazarroNodeIdentifier(peer.getIdentity().toByteArray());
				InetSocketAddress ipEndpoint=new InetSocketAddress(peer.getIpEndpoint().getIpaddress(), peer.getIpEndpoint().getPort());
				nodeDb.mapNodeIdToAddress(bazarroNodeIdentifier, ipEndpoint);
			}
		}
	}
}
