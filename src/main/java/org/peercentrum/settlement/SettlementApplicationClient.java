package org.peercentrum.settlement;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.bitcoin.paymentchannel.Protos;
import org.bitcoin.paymentchannel.Protos.TwoWayChannelMessage;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.SettlementMsg;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.WalletExtension;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.protocols.channels.IPaymentChannelClient.ClientConnection;
import com.google.bitcoin.protocols.channels.PaymentChannelClient;
import com.google.bitcoin.protocols.channels.PaymentChannelCloseException.CloseReason;
import com.google.bitcoin.protocols.channels.StoredPaymentChannelClientStates;
import com.google.common.collect.ImmutableList;

public class SettlementApplicationClient implements Closeable {
  private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplicationClient.class);
  
  
  WalletAppKit clientKit;
  StoredPaymentChannelClientStates clientStoredStates;
  NetworkClient networkClient;
  SettlementMethodTable settlementMethodTable;
  Object paymentChannelIsOpenMonitor=new Object();
  ClientConnection clientHandler=new ClientConnection(){
    @Override
    public void sendToServer(TwoWayChannelMessage twoWayClientMsg) {
      System.out.println("CLIENT: send to server "+twoWayClientMsg);
      ProtocolBuffer.SettlementMsg.Builder saRequestMsg=ProtocolBuffer.SettlementMsg.newBuilder();
      saRequestMsg.addTwoWayChannelMsg(twoWayClientMsg);
      Future<SettlementMsg> responseFuture = networkClient.sendRequest(serverNodeId, SettlementApplication.APP_ID, saRequestMsg.build());
      responseFuture.addListener(new GenericFutureListener<Future<SettlementMsg>>() {
        @Override
        public void operationComplete(Future<SettlementMsg> future) throws Exception {
          ProtocolBuffer.SettlementMsg serverResponseMsg=future.get();
          if(serverResponseMsg.getTwoWayChannelMsgCount()==0){
            LOGGER.error("Received a settlement response without a TwoWayChannelMsg");
            return;
          }
          for(Protos.TwoWayChannelMessage twoWayServerMsg : serverResponseMsg.getTwoWayChannelMsgList()){
            LOGGER.debug("Client received response from server {}", twoWayServerMsg);
            paymentClient.receiveMessage(twoWayServerMsg);
          }
          LOGGER.debug("client Done processing response");
        }
      });
    }

    @Override
    public void destroyConnection(CloseReason reason) {
      System.out.println("CLIENT: destroyConnection "+reason);
    }

    @Override
    public void channelOpen(boolean wasInitiated) {
      System.out.println("CLIENT: Channel open "+wasInitiated);
      synchronized (paymentChannelIsOpenMonitor) {
        paymentChannelIsOpenMonitor.notifyAll();
      }
    }
  };
  private PaymentChannelClient paymentClient;
  private NodeIdentifier serverNodeId;

  public SettlementApplicationClient(NetworkClient networkClient, TopLevelConfig topConfig, SettlementMethodTable settlementMethodTable) {
    this.networkClient=networkClient;
    this.settlementMethodTable=settlementMethodTable;
    RegTestParams params=RegTestParams.get(); //FIXME
    File bitcoinDir=topConfig.getFile("bitcoin");
    clientKit=new WalletAppKit(params, bitcoinDir, "settlementClient") {
      @Override
      protected List<WalletExtension> provideWalletExtensions() throws Exception {
        clientStoredStates = new StoredPaymentChannelClientStates(wallet(), peerGroup());
        return ImmutableList.of((WalletExtension) clientStoredStates);
      }
    };
    if(params==RegTestParams.get()){
      clientKit.connectToLocalHost();
    }
    clientKit.startAsync().awaitRunning();
  }

  public void openPaymentChannelTo(NodeIdentifier otherNode, Coin escrowAmount) throws Exception{
    ECKey clientKey=clientKit.wallet().freshReceiveKey();
    Sha256Hash contractId=Sha256Hash.create(otherNode.getBytes());
    serverNodeId=otherNode;
    paymentClient=new PaymentChannelClient(clientKit.wallet(), clientKey, escrowAmount, contractId, clientHandler);
    synchronized(paymentChannelIsOpenMonitor){
      paymentClient.connectionOpen();
      paymentChannelIsOpenMonitor.wait();
    }
  }

  //    BigInteger amountPlusFee = escrowAmount.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB);
//    
//    ECKey serverPublicKey=settlementMethodTable.getBitcoinSettlementMethod(otherNode);
//    
//    ECKey clientEscrowKey=new ECKey();
//    long channelExpirationInSeconds=(System.currentTimeMillis()/1000)+(24*60*60);
//    //  channelExpirationInSeconds=(System.currentTimeMillis()/1000); //FIXME Allow immediate refund for testing purposes
//    PaymentChannelClientState clientState = new PaymentChannelClientState(clientKit.wallet(), clientEscrowKey,
//        serverPublicKey, amountPlusFee, channelExpirationInSeconds);
//    clientState.initiate();
//    byte[] unsignedRefundTxFromClient = clientState.getIncompleteRefundTransaction().bitcoinSerialize();
//
//  }

  @Override
  public void close() throws IOException {
    paymentClient.connectionClosed();
    clientKit.stopAsync().awaitTerminated();
  }

  public Coin getAmountRemainingInChannel(NodeIdentifier remoteNodeId) {
    return clientStoredStates.getBalanceForServer(Sha256Hash.create(remoteNodeId.getBytes()));
  }

  public void makeMicroPayment(NodeIdentifier localNodeId, ApplicationIdentifier appId, Coin microPaymentAmount) throws Exception {
    paymentClient.incrementPayment(microPaymentAmount).get();
    LOGGER.debug("made microPayment of amount {}", microPaymentAmount);
  }
}
