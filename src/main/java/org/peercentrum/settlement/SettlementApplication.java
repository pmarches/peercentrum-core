package org.peercentrum.settlement;

import org.bitcoin.paymentchannel.Protos;
import org.peercentrum.application.BaseApplicationMessageHandler;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.HeaderMsg.Builder;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.ServerMain;
import org.peercentrum.network.HeaderAndPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class SettlementApplication extends BaseApplicationMessageHandler {
	public static final String BITCOIN_WALLET_FILENAME = "bitcoin.wallet";
  private static final String SETTLEMENT_DB_FILENAME = "settlement.db";
  private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier("SettlementApplication".getBytes());

  protected SettlementDB db;
  protected BitcoinSettlement bitcoinSettlement;
	
	public SettlementApplication(ServerMain serverMain) throws Exception {
		super(serverMain);
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		LOGGER.debug("settlement generateReponseFromQuery");
		try {
			NodeIdentifier remoteNodeIdentifier=serverMain.getNetworkServer().getRemoteNodeIdentifier(ctx);

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
			    getBitcoinSettlement().handleTwoWayMessage(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
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
	
	protected BitcoinSettlement getBitcoinSettlement() throws Exception{
	  if(bitcoinSettlement==null){
	    db=new SettlementDB(serverMain.getConfig().getFileRelativeFromConfigDirectory(SETTLEMENT_DB_FILENAME));
	    bitcoinSettlement=new BitcoinSettlement(serverMain.getConfig().getFileRelativeFromConfigDirectory(BITCOIN_WALLET_FILENAME));
	  }
	  return bitcoinSettlement;
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

//	public void exchangeSettlementAddresses(final NetworkClient client, NodeIdentifier remoteEndpoint) throws Exception {
//		PB.SettlementMessage.Builder topLevelSettlementMsg=PB.SettlementMessage.newBuilder();
//		topLevelSettlementMsg.setRequestSettlementMethod(true);
//		
//		PB.SettlementMethod.Builder localSettlementMethod = getLocalSettlementMethod();
//		topLevelSettlementMsg.setSettlementMethod(localSettlementMethod);
//		
//		Future<PB.SettlementMessage> settlementResponseFuture = client.sendRequest(remoteEndpoint, getApplicationId(), topLevelSettlementMsg.build());
//		PB.SettlementMessage settlementResponse=settlementResponseFuture.get();
//		if(settlementResponse.hasSettlementMethod()){
//			PB.SettlementMethod remoteSettlementMethod=settlementResponse.getSettlementMethod();
//			recordSettlementMehod(remoteEndpoint, remoteSettlementMethod);
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
