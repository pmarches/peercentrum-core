package org.peercentrum.settlement;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.SenderInformationMsg;
import org.peercentrum.core.ProtocolBuffer.SettlementMethod;
import org.peercentrum.core.ProtocolBuffer.HeaderMessage.Builder;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class SettlementApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier("SettlementApplication".getBytes());

//	RippleSeedAddress rippleSeed;
//	RippleSettlementDB rippleSettlement;
	
	public SettlementApplication(NetworkServer clientOrServer, SettlementConfig config) throws Exception {
		super(clientOrServer);
//		rippleSeed=new RippleSeedAddress(config.getRippleSeed());
//		rippleSettlement=new RippleSettlementDB(rippleSeed.getPublicRippleAddress(), config.getSettlementDbPath());
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		LOGGER.debug("settlement generateReponseFromQuery");

		try {
			if(receivedMessage.header.hasSenderInfo()==false){
				return null;
			}
			SenderInformationMsg senderInfo = receivedMessage.header.getSenderInfo();
			NodeIdentifier remoteNodeIdentifier = new NodeIdentifier(senderInfo.getNodePublicKey().toByteArray());

			ProtocolBuffer.SettlementMessage settlementMessage = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, ProtocolBuffer.SettlementMessage.class);
			if(settlementMessage.hasSettlementMethod()){
				SettlementMethod remoteSettlementMethod = settlementMessage.getSettlementMethod();
				recordSettlementMehod(remoteNodeIdentifier, remoteSettlementMethod);
			}
			
			ProtocolBuffer.SettlementMessage.Builder topLevelResponse=ProtocolBuffer.SettlementMessage.newBuilder();
			if(settlementMessage.hasRequestSettlementMethod() && settlementMessage.getRequestSettlementMethod()){
				ProtocolBuffer.SettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
				topLevelResponse.setSettlementMethod(localSettlementMethod);
			}
			
			
			Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			ByteBuf payloadBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(topLevelResponse.build());
			return new HeaderAndPayload(headerResponse, payloadBytes);
		} catch (Exception e) {
			LOGGER.error("Exception when decoding settlement message ", e);
			return null;
		}
	}
	
	void recordSettlementMehod(NodeIdentifier remoteNodeIdentifier, SettlementMethod settlementMethod) throws Exception {
		LOGGER.debug("Recording settlement method for node {} ", remoteNodeIdentifier);
		if(settlementMethod.hasBitcoinAddress()){
			LOGGER.error("Bitcoin micro-payment settlement not implemented yet :-(");
		}
		if(settlementMethod.hasRippleAddress()){
//			RippleAddress remoteRippleAddress=new RippleAddress(settlementMethod.getRippleAddress().toByteArray());
//			rippleSettlement.setSettlementMethod(remoteNodeIdentifier, remoteRippleAddress);
		}
	}

	public void exchangeSettlementAddresses(final NetworkClient client, NodeIdentifier remoteNodeId) throws Exception {
		ProtocolBuffer.SettlementMessage.Builder topLevelSettlementMsg=ProtocolBuffer.SettlementMessage.newBuilder();
		topLevelSettlementMsg.setRequestSettlementMethod(true);
		
		ProtocolBuffer.SettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
		topLevelSettlementMsg.setSettlementMethod(localSettlementMethod);
		
		Future<ProtocolBuffer.SettlementMessage> settlementResponseFuture = client.sendRequest(remoteNodeId, getApplicationId(), topLevelSettlementMsg.build());
		ProtocolBuffer.SettlementMessage settlementResponse=settlementResponseFuture.get();
		if(settlementResponse.hasSettlementMethod()){
			ProtocolBuffer.SettlementMethod remoteSettlementMethod=settlementResponse.getSettlementMethod();
			recordSettlementMehod(remoteNodeId, remoteSettlementMethod);
		}
	}

	protected ProtocolBuffer.SettlementMethod.Builder getLocalSettlementMethod() {
		ProtocolBuffer.SettlementMethod.Builder localSettlementMethod=ProtocolBuffer.SettlementMethod.newBuilder();
		localSettlementMethod.addAcceptedRippleCurrencies(ByteString.EMPTY);
//		RippleAddress localRippleAddress=rippleSeed.getPublicRippleAddress();
//		localSettlementMethod.setRippleAddress(ByteString.copyFrom(localRippleAddress.getBytes()));
		return localSettlementMethod;
	}

	@Override
	public ApplicationIdentifier getApplicationId() {
		return APP_ID;
	}

}
