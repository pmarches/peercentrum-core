package org.peercentrum.settlement;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.bitcoin.paymentchannel.Protos;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg.Builder;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettlementApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier("SettlementApplication".getBytes());

  protected SettlementDB db;
  protected BitcoinSettlement bitcoinSettlement;
	
	public SettlementApplication(NetworkServer server) throws Exception {
		super(server);
    db=new SettlementDB(server.getConfig().getFile("settlement.db"));
    bitcoinSettlement=new BitcoinSettlement(server.getConfig().getFile("bitcoin.wallet"));
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		LOGGER.debug("settlement generateReponseFromQuery");
		try {
			NodeIdentifier remoteNodeIdentifier=super.getRemoteNodeIdentifier(ctx);

      PB.SettlementMsg.Builder topLevelResponse=PB.SettlementMsg.newBuilder();
			PB.SettlementMsg settlementReqMsg = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, PB.SettlementMsg.class);
//			if(settlementReqMsg.hasCreateMicroPaymentChannelMsg()){
//			  PB.CreateMicroPaymentChannelMsg createChannelMsg=settlementReqMsg.getCreateMicroPaymentChannelMsg();
//			  bitcoinSettlement.createNewMicroPaymentChannel(createChannelMsg, topLevelResponse);
//			}
//			if(settlementReqMsg.hasMicroPaymentUpdateMsg()){
//			  PB.MicroPaymentUpdateMsg updateMicroPymtMsg=settlementReqMsg.getMicroPaymentUpdateMsg();
//			  updateMicropaymentChannel(updateMicroPymtMsg, topLevelResponse);
//			}
			if(settlementReqMsg.getTwoWayChannelMsgCount()>0){
			  for(Protos.TwoWayChannelMessage twoWayMsg : settlementReqMsg.getTwoWayChannelMsgList()){
			    bitcoinSettlement.handleTwoWayMessage(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
			  }
			}
			
			Builder headerResponse = super.newResponseHeaderForRequest(receivedMessage);
			ByteBuf payloadBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(topLevelResponse.build());
			return new HeaderAndPayload(headerResponse, payloadBytes);
		} catch (Exception e) {
			LOGGER.error("Exception when decoding settlement message ", e);
			return null;
		}
	}
	
//	void recordSettlementMehod(NodeIdentifier remoteNodeIdentifier, SettlementMethod settlementMethod) throws Exception {
//		LOGGER.debug("Recording settlement method for node {} ", remoteNodeIdentifier);
//		if(settlementMethod.hasBitcoinPublicKey()){
//			LOGGER.error("Bitcoin micro-payment settlement not implemented yet :-(");
//			ECKey remoteBitcointPublicKey=new ECKey(null, settlementMethod.getBitcoinPublicKey().toByteArray());
//			db.settlementMethod.setBitcointSettlementMethod(remoteNodeIdentifier, remoteBitcointPublicKey);
//		}
//		if(settlementMethod.hasRippleAddress()){
////			RippleAddress remoteRippleAddress=new RippleAddress(settlementMethod.getRippleAddress().toByteArray());
////			rippleSettlement.setSettlementMethod(remoteNodeIdentifier, remoteRippleAddress);
//		}
//	}

//	public void exchangeSettlementAddresses(final NetworkClient client, NodeIdentifier remoteNodeId) throws Exception {
//		PB.SettlementMessage.Builder topLevelSettlementMsg=PB.SettlementMessage.newBuilder();
//		topLevelSettlementMsg.setRequestSettlementMethod(true);
//		
//		PB.SettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
//		topLevelSettlementMsg.setSettlementMethod(localSettlementMethod);
//		
//		Future<PB.SettlementMessage> settlementResponseFuture = client.sendRequest(remoteNodeId, getApplicationId(), topLevelSettlementMsg.build());
//		PB.SettlementMessage settlementResponse=settlementResponseFuture.get();
//		if(settlementResponse.hasSettlementMethod()){
//			PB.SettlementMethod remoteSettlementMethod=settlementResponse.getSettlementMethod();
//			recordSettlementMehod(remoteNodeId, remoteSettlementMethod);
//		}
//	}
//
//	protected PB.SettlementMethod.Builder getLocalSettlementMethod() {
//		PB.SettlementMethod.Builder localSettlementMethod=PB.SettlementMethod.newBuilder();
//		localSettlementMethod.addAcceptedRippleCurrencies(ByteString.EMPTY);
////		RippleAddress localRippleAddress=rippleSeed.getPublicRippleAddress();
////		localSettlementMethod.setRippleAddress(ByteString.copyFrom(localRippleAddress.getBytes()));
//		return localSettlementMethod;
//	}

  @Override
	public ApplicationIdentifier getApplicationId() {
		return APP_ID;
	}

}
