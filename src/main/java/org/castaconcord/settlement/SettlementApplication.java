package org.castaconcord.settlement;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;

import org.castaconcord.core.BazarroApplicationIdentifier;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.BazarroHeaderMessage.Builder;
import org.castaconcord.core.ProtocolBuffer.BazarroSettlementMethod;
import org.castaconcord.core.ProtocolBuffer.SenderInformationMsg;
import org.castaconcord.network.BaseBazarroApplicationMessageHandler;
import org.castaconcord.network.BazarroNetworkClient;
import org.castaconcord.network.BazarroNetworkServer;
import org.castaconcord.network.HeaderAndPayload;
import org.castaconcord.network.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class SettlementApplication extends BaseBazarroApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplication.class);
	public static final BazarroApplicationIdentifier APP_ID=new BazarroApplicationIdentifier("SettlementApplication".getBytes());

//	RippleSeedAddress rippleSeed;
//	RippleSettlementDB rippleSettlement;
	
	public SettlementApplication(BazarroNetworkServer clientOrServer, SettlementConfig config) throws Exception {
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
			BazarroNodeIdentifier remoteNodeIdentifier = new BazarroNodeIdentifier(senderInfo.getNodePublicKey().toByteArray());

			ProtocolBuffer.BazarroSettlementMessage settlementMessage = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, ProtocolBuffer.BazarroSettlementMessage.class);
			if(settlementMessage.hasSettlementMethod()){
				BazarroSettlementMethod remoteSettlementMethod = settlementMessage.getSettlementMethod();
				recordSettlementMehod(remoteNodeIdentifier, remoteSettlementMethod);
			}
			
			ProtocolBuffer.BazarroSettlementMessage.Builder topLevelResponse=ProtocolBuffer.BazarroSettlementMessage.newBuilder();
			if(settlementMessage.hasRequestSettlementMethod() && settlementMessage.getRequestSettlementMethod()){
				ProtocolBuffer.BazarroSettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
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
	
	void recordSettlementMehod(BazarroNodeIdentifier remoteNodeIdentifier, BazarroSettlementMethod settlementMethod) throws Exception {
		LOGGER.debug("Recording settlement method for node {} ", remoteNodeIdentifier);
		if(settlementMethod.hasBitcoinAddress()){
			LOGGER.error("Bitcoin micro-payment settlement not implemented yet :-(");
		}
		if(settlementMethod.hasRippleAddress()){
//			RippleAddress remoteRippleAddress=new RippleAddress(settlementMethod.getRippleAddress().toByteArray());
//			rippleSettlement.setSettlementMethod(remoteNodeIdentifier, remoteRippleAddress);
		}
	}

	public void exchangeSettlementAddresses(final BazarroNetworkClient client, BazarroNodeIdentifier remoteNodeId) throws Exception {
		ProtocolBuffer.BazarroSettlementMessage.Builder topLevelSettlementMsg=ProtocolBuffer.BazarroSettlementMessage.newBuilder();
		topLevelSettlementMsg.setRequestSettlementMethod(true);
		
		ProtocolBuffer.BazarroSettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
		topLevelSettlementMsg.setSettlementMethod(localSettlementMethod);
		
		Future<ProtocolBuffer.BazarroSettlementMessage> settlementResponseFuture = client.sendRequest(remoteNodeId, getApplicationId(), topLevelSettlementMsg.build());
		ProtocolBuffer.BazarroSettlementMessage settlementResponse=settlementResponseFuture.get();
		if(settlementResponse.hasSettlementMethod()){
			ProtocolBuffer.BazarroSettlementMethod remoteSettlementMethod=settlementResponse.getSettlementMethod();
			recordSettlementMehod(remoteNodeId, remoteSettlementMethod);
		}
	}

	protected ProtocolBuffer.BazarroSettlementMethod.Builder getLocalSettlementMethod() {
		ProtocolBuffer.BazarroSettlementMethod.Builder localSettlementMethod=ProtocolBuffer.BazarroSettlementMethod.newBuilder();
		localSettlementMethod.addAcceptedRippleCurrencies(ByteString.EMPTY);
//		RippleAddress localRippleAddress=rippleSeed.getPublicRippleAddress();
//		localSettlementMethod.setRippleAddress(ByteString.copyFrom(localRippleAddress.getBytes()));
		return localSettlementMethod;
	}

	@Override
	public BazarroApplicationIdentifier getApplicationId() {
		return APP_ID;
	}

}
