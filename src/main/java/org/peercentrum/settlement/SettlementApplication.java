package org.peercentrum.settlement;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.bitcoin.paymentchannel.Protos;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.HeaderMessage.Builder;
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
		SettlementConfig settlementConfig=(SettlementConfig) server.getConfig().getAppConfig(SettlementConfig.class);
    db=new SettlementDB(server.getConfig().getFile(settlementConfig.getSettlementDbPath()));
    bitcoinSettlement=new BitcoinSettlement(server.getConfig().getFile(settlementConfig.getBitcoinWalletPath()));
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		LOGGER.debug("settlement generateReponseFromQuery");
		try {
			if(receivedMessage.header.hasNodePublicKey()==false){
				return null;
			}
			NodeIdentifier remoteNodeIdentifier = new NodeIdentifier(receivedMessage.header.getNodePublicKey().toByteArray());

      ProtocolBuffer.SettlementMsg.Builder topLevelResponse=ProtocolBuffer.SettlementMsg.newBuilder();
			ProtocolBuffer.SettlementMsg settlementReqMsg = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, ProtocolBuffer.SettlementMsg.class);
//			if(settlementReqMsg.hasCreateMicroPaymentChannelMsg()){
//			  ProtocolBuffer.CreateMicroPaymentChannelMsg createChannelMsg=settlementReqMsg.getCreateMicroPaymentChannelMsg();
//			  bitcoinSettlement.createNewMicroPaymentChannel(createChannelMsg, topLevelResponse);
//			}
//			if(settlementReqMsg.hasMicroPaymentUpdateMsg()){
//			  ProtocolBuffer.MicroPaymentUpdateMsg updateMicroPymtMsg=settlementReqMsg.getMicroPaymentUpdateMsg();
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
//		ProtocolBuffer.SettlementMessage.Builder topLevelSettlementMsg=ProtocolBuffer.SettlementMessage.newBuilder();
//		topLevelSettlementMsg.setRequestSettlementMethod(true);
//		
//		ProtocolBuffer.SettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
//		topLevelSettlementMsg.setSettlementMethod(localSettlementMethod);
//		
//		Future<ProtocolBuffer.SettlementMessage> settlementResponseFuture = client.sendRequest(remoteNodeId, getApplicationId(), topLevelSettlementMsg.build());
//		ProtocolBuffer.SettlementMessage settlementResponse=settlementResponseFuture.get();
//		if(settlementResponse.hasSettlementMethod()){
//			ProtocolBuffer.SettlementMethod remoteSettlementMethod=settlementResponse.getSettlementMethod();
//			recordSettlementMehod(remoteNodeId, remoteSettlementMethod);
//		}
//	}
//
//	protected ProtocolBuffer.SettlementMethod.Builder getLocalSettlementMethod() {
//		ProtocolBuffer.SettlementMethod.Builder localSettlementMethod=ProtocolBuffer.SettlementMethod.newBuilder();
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
