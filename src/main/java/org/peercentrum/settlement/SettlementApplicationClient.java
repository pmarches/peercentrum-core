package org.peercentrum.settlement;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bitcoin.paymentchannel.Protos;
import org.bitcoin.paymentchannel.Protos.TwoWayChannelMessage;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.protocols.channels.IPaymentChannelClient.ClientConnection;
import org.bitcoinj.protocols.channels.PaymentChannelClient;
import org.bitcoinj.protocols.channels.PaymentChannelCloseException.CloseReason;
import org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.SettlementMsg;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class SettlementApplicationClient implements Closeable {
  public static final String BITCOIN_DIRNAME = "bitcoin";

  private static final Logger LOGGER = LoggerFactory.getLogger(SettlementApplicationClient.class);
  
  WalletAppKit clientKit;
  StoredPaymentChannelClientStates clientStoredStates;
  NetworkClientConnection networkClient;
  SettlementMethodTable settlementMethodTable;
  Object paymentChannelIsOpenMonitor=new Object();
  Object paymentChannelIsClosed=new Object();
  ClientConnection clientHandler=new ClientConnection(){
    @Override
    public void sendToServer(TwoWayChannelMessage twoWayClientMsg) {
      LOGGER.debug("CLIENT: send to networkServer {}", twoWayClientMsg);
      PB.SettlementMsg.Builder saRequestMsg=PB.SettlementMsg.newBuilder();
      saRequestMsg.addTwoWayChannelMsg(twoWayClientMsg);
      Future<SettlementMsg> responseFuture = networkClient.sendRequestMsg(SettlementApplication.APP_ID, saRequestMsg.build());
      responseFuture.addListener(new GenericFutureListener<Future<SettlementMsg>>() {
        @Override
        public void operationComplete(Future<SettlementMsg> future) throws Exception {
          PB.SettlementMsg serverResponseMsg=future.get();
          if(serverResponseMsg.getTwoWayChannelMsgCount()==0){
            LOGGER.error("Received a settlement response without a TwoWayChannelMsg");
            return;
          }
          for(Protos.TwoWayChannelMessage twoWayServerMsg : serverResponseMsg.getTwoWayChannelMsgList()){
            LOGGER.debug("Client received response from networkServer {}", twoWayServerMsg);
            paymentChannel.receiveMessage(twoWayServerMsg);
          }
          LOGGER.debug("client Done processing response");
        }
      });
    }

    @Override
    public void destroyConnection(CloseReason reason) {
      System.out.println("CLIENT: destroyConnection "+reason);
      paymentChannel.connectionClosed();
      synchronized (paymentChannelIsClosed) {
        paymentChannelIsClosed.notifyAll();
      }
    }

    @Override
    public void channelOpen(boolean wasInitiated) {
      System.out.println("CLIENT: Channel open "+wasInitiated);
      synchronized (paymentChannelIsOpenMonitor) {
        paymentChannelIsOpenMonitor.notifyAll();
      }
    }

    @Override
    public boolean acceptExpireTime(long expireTime) {
      System.out.println("CLIENT: acceptExpireTime "+expireTime);
      return false;
    }
  };
  private PaymentChannelClient paymentChannel;

  public SettlementApplicationClient(NetworkClientConnection networkClient, TopLevelConfig topConfig, SettlementMethodTable settlementMethodTable) {
    this.networkClient=networkClient;
    this.settlementMethodTable=settlementMethodTable;
    RegTestParams params=RegTestParams.get(); //FIXME
    File bitcoinDir=topConfig.getFileRelativeFromConfigDirectory(BITCOIN_DIRNAME);
    clientKit=new WalletAppKit(params, bitcoinDir, "settlementClient1") {
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

  public void openPaymentChannel(NodeIdentifier otherNode, Coin escrowAmount) throws Exception{
    ECKey clientKey=clientKit.wallet().freshReceiveKey();
    Sha256Hash contractId=Sha256Hash.create(otherNode.getBytes());
    paymentChannel=new PaymentChannelClient(clientKit.wallet(), clientKey, escrowAmount, contractId, clientHandler);
    synchronized(paymentChannelIsOpenMonitor){
      paymentChannel.connectionOpen();
      paymentChannelIsOpenMonitor.wait();
    }
  }

  @Override
  public void close() throws IOException {
    if(paymentChannel!=null){
      paymentChannel.connectionClosed();
    }
    clientKit.stopAsync().awaitTerminated();
  }

  public Coin getAmountRemainingInChannel(NodeIdentifier remoteNodeId) {
    return clientStoredStates.getBalanceForServer(Sha256Hash.create(remoteNodeId.getBytes()));
  }

  public void makeMicroPayment(ApplicationIdentifier appId, Coin microPaymentAmount) throws Exception {
    paymentChannel.incrementPayment(microPaymentAmount).get();
    LOGGER.debug("made microPayment of amount {}", microPaymentAmount);
  }

  public void closePaymentChannel(NodeIdentifier localNodeId) throws InterruptedException {
    paymentChannel.settle();
    synchronized(paymentChannelIsClosed){
      paymentChannelIsClosed.wait();
    }
  }
}
