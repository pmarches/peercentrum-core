package org.peercentrum.settlement;

import java.io.File;
import java.util.List;

import org.bitcoin.paymentchannel.Protos;
import org.bitcoin.paymentchannel.Protos.TwoWayChannelMessage;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtocolBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.WalletExtension;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.protocols.channels.PaymentChannelCloseException.CloseReason;
import com.google.bitcoin.protocols.channels.PaymentChannelServer;
import com.google.bitcoin.protocols.channels.PaymentChannelServer.ServerConnection;
import com.google.bitcoin.protocols.channels.StoredPaymentChannelServerStates;
import com.google.common.collect.ImmutableList;

public class BitcoinSettlement {
  private static final Logger LOGGER = LoggerFactory.getLogger(BitcoinSettlement.class);

  protected WalletAppKit serverKit;
  protected StoredPaymentChannelServerStates serverStoredStates;
//  /**
//   * <p>The amount of time we request the client lock in their funds.</p>
//   *
//   * <p>The value defaults to 24 hours - 60 seconds and should always be greater than 2 hours plus the amount of time
//   * the channel is expected to be used and smaller than 24 hours minus the client <-> server latency minus some
//   * factor to account for client clock inaccuracy.</p>
//   */
//  public long timeWindow = 24*60*60 - 60;
//  private DeterministicKey serverEscrowKey;
//  private long giveFullRefundToClientTS;
//  private PaymentChannelServerState state;
  private Coin minAcceptedChannelSize=Coin.valueOf(0, 1);

  private ServerConnection serverConnection=new ServerConnection() {
    @Override public void sendToClient(TwoWayChannelMessage msg) {
      LOGGER.debug("SERVER: sendToClient {}", msg);
      pendingTopLevelMsg.addTwoWayChannelMsg(msg);
    }
    
    @Override public void paymentIncrease(Coin by, Coin to) {
      LOGGER.debug("SERVER: paymentIncrease by={} to={}", by, to);
    }
    
    @Override public void destroyConnection(CloseReason reason) {
      LOGGER.debug("SERVER: destroyConnection because {}", reason);
    }
    
    @Override public void channelOpen(Sha256Hash contractHash) {
      LOGGER.debug("SERVER: channelOpen {}", contractHash);
    }
  };
  ProtocolBuffer.SettlementMsg.Builder pendingTopLevelMsg;

  private PaymentChannelServer paymentChannelServer;
  
  public BitcoinSettlement(File walletDirectory) throws Exception {
    NetworkParameters params=RegTestParams.get();
    serverKit=new WalletAppKit(params, walletDirectory, "serverWallet"){
      @Override
      protected List<WalletExtension> provideWalletExtensions() throws Exception {
        serverStoredStates=new StoredPaymentChannelServerStates(wallet(), peerGroup());
        return ImmutableList.of((WalletExtension)serverStoredStates);
      }
    };
    serverKit.setUserAgent("peercentrum", "0.1");
    if(params==RegTestParams.get()){
      serverKit.connectToLocalHost();
    }
    serverKit.setBlockingStartup(false);
    serverKit.startAsync(); //TODO Add listener here
    serverKit.awaitRunning();
    paymentChannelServer=new PaymentChannelServer(serverKit.peerGroup(), serverKit.wallet(), minAcceptedChannelSize, serverConnection);
    paymentChannelServer.connectionOpen();
  }

  public void handleTwoWayMessage(NodeIdentifier remoteNodeIdentifier, Protos.TwoWayChannelMessage twoWayMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
    if(twoWayMsg.hasType()==false){
      throw new Exception("Missing message type");
    }
    pendingTopLevelMsg=topLevelResponse;
    paymentChannelServer.receiveMessage(twoWayMsg);
    pendingTopLevelMsg=null;
  }
//    switch(twoWayMsg.getType()){
//    case CLIENT_VERSION:
//      handleClientVersion(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
//      return;
//    case PROVIDE_REFUND:
//      handleProvideRefund(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
//      return;
//    case PROVIDE_CONTRACT:
//      handleReceiveContract(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
//      return;
//    case UPDATE_PAYMENT:
//      handleUpdatePayment(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
//      return;
//    case CLOSE:
//      handleClose(remoteNodeIdentifier, twoWayMsg, topLevelResponse);
//      return;
//    case ERROR:
//      checkState(twoWayMsg.hasError());
//      LOGGER.error("Client sent ERROR {} with explanation {}", twoWayMsg.getError().getCode().name(),
//          twoWayMsg.getError().hasExplanation() ? twoWayMsg.getError().getExplanation() : "");
//      //        conn.destroyConnection(CloseReason.REMOTE_SENT_ERROR);
//      return;
//    default:
//      final String errorText = "Got unknown message type or type that doesn't apply to servers.";
//      LOGGER.error(errorText, Protos.Error.ErrorCode.SYNTAX_ERROR, CloseReason.REMOTE_SENT_INVALID_MESSAGE);
//    }
//  }
//
//  private void handleClientVersion(NodeIdentifier remoteNodeIdentifier, Protos.TwoWayChannelMessage twoWayMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//    Protos.ClientVersion clientVersion = twoWayMsg.getClientVersion();
//    if(clientVersion.hasPreviousChannelContractHash()){
//      //TODO handle channel re-use?
//      throw new Exception("not implemented");
//    }
//
//    giveFullRefundToClientTS=Utils.currentTimeSeconds() + timeWindow;
//    serverEscrowKey=serverKit.wallet().freshReceiveKey();
//    minAcceptedChannelSize=Coin.valueOf(0, 1);
//
//    Protos.Initiate.Builder initiateBuilder = Protos.Initiate.newBuilder()
//        .setMultisigKey(ByteString.copyFrom(serverEscrowKey.getPubKey()))
//        .setExpireTimeSecs(giveFullRefundToClientTS)
//        .setMinAcceptedChannelSize(minAcceptedChannelSize.value)
//        .setMinPayment(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.value);
//    Protos.TwoWayChannelMessage.Builder twoWayResponseMsg=Protos.TwoWayChannelMessage.newBuilder()
//        .setInitiate(initiateBuilder)
//        .setType(MessageType.INITIATE);
//    topLevelResponse.setTwoWayChannelMsg(twoWayResponseMsg);
//  }
//
//  private void handleProvideRefund(NodeIdentifier remoteNodeIdentifier, Protos.TwoWayChannelMessage twoWayMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//    if(twoWayMsg.hasProvideRefund()==false){
//      throw new Exception("Missing provide refund message");
//    }
//
//    state = new PaymentChannelServerState(serverKit.peerGroup(), serverKit.wallet(), serverEscrowKey, this.giveFullRefundToClientTS);
//
//    Protos.ProvideRefund providedRefund = twoWayMsg.getProvideRefund();
//    byte[] clientSidePublicKey=providedRefund.getMultisigKey().toByteArray();
//    byte[] unsignedTXBytes = providedRefund.getTx().toByteArray();
//    Transaction unsignedTransaction=new Transaction(serverKit.params(), unsignedTXBytes);
//    byte[] signature = state.provideRefundTransaction(unsignedTransaction, clientSidePublicKey);
//
//    Protos.ReturnRefund.Builder returnRefundBuilder = Protos.ReturnRefund.newBuilder()
//        .setSignature(ByteString.copyFrom(signature));
//
//    Protos.TwoWayChannelMessage.Builder twoWayResponseMsg=Protos.TwoWayChannelMessage.newBuilder()
//        .setReturnRefund(returnRefundBuilder)
//        .setType(Protos.TwoWayChannelMessage.MessageType.RETURN_REFUND);
//    topLevelResponse.setTwoWayChannelMsg(twoWayResponseMsg);
//  }
//
//  private void handleReceiveContract(NodeIdentifier remoteNodeIdentifier, Protos.TwoWayChannelMessage twoWayMsg, final ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//    if(twoWayMsg.hasProvideContract()==false){
//      throw new Exception("Missing provide contract");
//    }
//    LOGGER.info("Got contract, broadcasting and responding with CHANNEL_OPEN");
//    final Protos.ProvideContract providedContract = twoWayMsg.getProvideContract();
//
//    //TODO notify connection handler that timeout should be significantly extended as we wait for network propagation?
//    final Transaction multisigContract = new Transaction(serverKit.params(), providedContract.getTx().toByteArray());
//    state.provideMultiSigContract(multisigContract)
//    .addListener(new Runnable() {
//      @Override
//      public void run() {
//        multisigContractPropagated(providedContract, multisigContract.getHash(), topLevelResponse);
//      }
//    }, Threading.SAME_THREAD);
//  }
//
//  private void handleUpdatePayment(Protos.UpdatePayment updatePaymentMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//    LOGGER.info("Got a payment update");
//
//    Coin lastBestPayment = state.getBestValueToMe();
//    final Coin refundSize = Coin.valueOf(updatePaymentMsg.getClientChangeValue());
//    boolean stillUsable = state.incrementPayment(refundSize, updatePaymentMsg.getSignature().toByteArray());
//    Coin bestPaymentChange = state.getBestValueToMe().subtract(lastBestPayment);
//
//    if (bestPaymentChange.signum() > 0)
//      conn.paymentIncrease(bestPaymentChange, state.getBestValueToMe());
//
//    if (topLevelResponse!=null) {
//      Protos.TwoWayChannelMessage.Builder ack = Protos.TwoWayChannelMessage.newBuilder();
//      ack.setType(Protos.TwoWayChannelMessage.MessageType.PAYMENT_ACK);
//      Protos.TwoWayChannelMessage.Builder twoWayResponse=Protos.TwoWayChannelMessage.newBuilder()
//          .setType(Protos.TwoWayChannelMessage.MessageType.PAYMENT_ACK);
//      topLevelResponse.setTwoWayChannelMsg(twoWayResponse);
//    }
//
//    if (!stillUsable) {
//      LOGGER.info("Channel is now fully exhausted, closing/initiating settlement");
//      settlePayment(CloseReason.CHANNEL_EXHAUSTED);
//    }
//  }
//
//  private void handleClose(NodeIdentifier remoteNodeIdentifier, Protos.TwoWayChannelMessage twoWayMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//
//  }
//
//  private void multisigContractPropagated(Protos.ProvideContract providedContract, Sha256Hash contractHash, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) {
//    state.storeChannelInWallet(null);
//    try {
//      handleUpdatePayment(providedContract.getInitialPayment(), null);
//    } catch (VerificationException e) {
//      LOGGER.error("Initial payment failed to verify", e);
//      return;
//    } catch (ValueOutOfRangeException e) {
//      LOGGER.error("Initial payment value was out of range", e);
//      return;
//    } catch (InsufficientMoneyException e) {
//      // This shouldn't happen because the server shouldn't allow itself to get into this situation in the
//      // first place, by specifying a min up front payment.
//      LOGGER.error("Tried to settle channel and could not afford the fees whilst updating payment", e);
//      return;
//    }
//    Protos.TwoWayChannelMessage.Builder twoWayResponse=Protos.TwoWayChannelMessage.newBuilder()
//        .setType(Protos.TwoWayChannelMessage.MessageType.CHANNEL_OPEN);
//    topLevelResponse.setTwoWayChannelMsg(twoWayResponse);
//  }
//
//  private void settlePayment(final CloseReason clientRequestedClose) throws InsufficientMoneyException {
//    // Setting channelSettling here prevents us from sending another CLOSE when state.close() calls
//    // close() on us here below via the stored channel state.
//    // TODO: Strongly separate the lifecycle of the payment channel from the TCP connection in these classes.
//    channelSettling = true;
//    Futures.addCallback(state.close(), new FutureCallback<Transaction>() {
//        @Override
//        public void onSuccess(Transaction result) {
//            // Send the successfully accepted transaction back to the client.
//            final Protos.TwoWayChannelMessage.Builder msg = Protos.TwoWayChannelMessage.newBuilder();
//            msg.setType(Protos.TwoWayChannelMessage.MessageType.CLOSE);
//            if (result != null) {
//                // Result can be null on various error paths, like if we never actually opened
//                // properly and so on.
//                msg.getSettlementBuilder().setTx(ByteString.copyFrom(result.bitcoinSerialize()));
//                log.info("Sending CLOSE back with broadcast settlement tx.");
//            } else {
//                log.info("Sending CLOSE back without broadcast settlement tx.");
//            }
//            conn.sendToClient(msg.build());
//            conn.destroyConnection(clientRequestedClose);
//        }
//
//        @Override
//        public void onFailure(Throwable t) {
//            log.error("Failed to broadcast settlement tx", t);
//            conn.destroyConnection(clientRequestedClose);
//        }
//    });
//}
//
//  public void createNewMicroPaymentChannel(ProtocolBuffer.CreateMicroPaymentChannelMsg createChannelMsg, ProtocolBuffer.SettlementMsg.Builder topLevelResponse) throws Exception {
//    if(createChannelMsg.hasTimeLockedFullRefundTX()==false){
//      throw new Exception("Missing the time locked full refund transaction bytes from the client");
//    }
//    if(createChannelMsg.hasClientSideEscrowPublicKey()==false){
//      throw new Exception("Missing the client side escrow public key bytes from the client");
//    }
//
//    long giveFullRefundToClientTS=Utils.currentTimeSeconds() + timeWindow;
//    //TODO Add lots of validation below
//    ECKey serverEscrowKey=serverKit.wallet().freshReceiveKey();
//    PaymentChannelServerState serverState = new PaymentChannelServerState(serverKit.peerGroup(), 
//        serverKit.wallet(), serverEscrowKey, giveFullRefundToClientTS);
//    byte[] unsignedRefundTxFromClient=createChannelMsg.getTimeLockedFullRefundSig().toByteArray();
//    Transaction unsignedFullRefundTX=new Transaction(serverKit.wallet().getParams(), unsignedRefundTxFromClient);
//    ECKey clientEscrowKey=ECKey.fromPublicOnly(createChannelMsg.getClientSideEscrowPublicKey().toByteArray());
//
//    byte[] fullRefundToClientSignature = serverState.provideRefundTransaction(unsignedFullRefundTX, clientEscrowKey.getPubKey());
//    ProtocolBuffer.CreateMicroPaymentChannelMsg.Builder createChannelResponseMsg=ProtocolBuffer.CreateMicroPaymentChannelMsg.newBuilder();
//    createChannelResponseMsg.setTimeLockedFullRefundSig(ByteString.copyFrom(fullRefundToClientSignature));
//    topLevelResponse.setCreateMicroPaymentChannelMsg(createChannelResponseMsg);
//  }

}
