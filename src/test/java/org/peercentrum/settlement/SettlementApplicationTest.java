package org.peercentrum.settlement;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TestUtilities;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;

import com.azazar.bitcoin.jsonrpcclient.BitcoinJSONRPCClient;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class SettlementApplicationTest {

  @Test
  public void testPayNode() throws Exception {
    BitcoinJSONRPCClient bitcoind = TestUtilities.getBitcoindRegtest();

    TopLevelConfig node2Config=TestUtilities.getConfig("node2"); //TODO Can we make this node transient?
    node2Config.setAppConfig(new SettlementConfig());
    NetworkServer server=new NetworkServer(node2Config);
    SettlementApplication settlementApp=new SettlementApplication(server);

    TopLevelConfig node1Config=TestUtilities.getConfig("node1");
    NodeDatabase nodeDatabase=new NodeDatabase(null);
    nodeDatabase.mapNodeIdToAddress(server.getLocalNodeId(), new InetSocketAddress(server.getListeningPort()));
    NetworkClient networkClient=new NetworkClient(new NodeIdentifier(node1Config.getNodeIdentifier()), nodeDatabase);
    SettlementDB clientSettlementDB=new SettlementDB(null);

    SettlementApplicationClient settlementClient=new SettlementApplicationClient(networkClient, node1Config, clientSettlementDB.settlementMethod);
    assertEquals(Coin.ZERO, settlementClient.getAmountRemainingInChannel(server.getLocalNodeId()));

    Coin escrowAmount=Coin.valueOf(0, 10);
    Coin amountPlusFee = escrowAmount.add(Wallet.SendRequest.DEFAULT_FEE_PER_KB);
    ListenableFuture<Coin> balanceFuture = settlementClient.clientKit.wallet().getBalanceFuture(amountPlusFee, Wallet.BalanceType.AVAILABLE);
    if (!balanceFuture.isDone()) {
      String clientAddress=settlementClient.clientKit.wallet().currentReceiveAddress().toString();
      bitcoind.sendToAddress(clientAddress, 10.0);
      bitcoind.setGenerate(true, 1);
      Futures.getUnchecked(balanceFuture);  // Wait.
    }
    settlementClient.openPaymentChannelTo(server.getLocalNodeId(), escrowAmount);
    assertEquals(escrowAmount.subtract(Wallet.SendRequest.DEFAULT_FEE_PER_KB), settlementClient.getAmountRemainingInChannel(server.getLocalNodeId()));

    settlementClient.makeMicroPayment(server.getLocalNodeId(), P2PBlobApplication.APP_ID, Coin.CENT);
    
    settlementClient.close();
    clientSettlementDB.close();
  }

}
