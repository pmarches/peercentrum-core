package org.peercentrum.settlement;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TransientMockNetworkOfNodes;
import org.peercentrum.network.NetworkClientConnection;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet.BalanceType;

public class SettlementApplicationTest {

  @Test
  public void testPayNode() throws Exception {
    TransientMockNetworkOfNodes mockNodes=new TransientMockNetworkOfNodes();
    SettlementDB clientSettlementDB=new SettlementDB(null);
    NetworkClientConnection clientToServerConnection = mockNodes.networkClient1.createConnectionToPeer(mockNodes.server1.getLocalNodeId());
    SettlementApplicationClient settlementClient=new SettlementApplicationClient(clientToServerConnection, mockNodes.client1Config, clientSettlementDB.settlementMethod);
    double clientStartAmount=13.0;
    mockNodes.fundBitcoinWalletOfNode(settlementClient.clientKit.wallet(), clientStartAmount);

    Coin escrow = Coin.valueOf(0, 10);
    Coin escrowPlusFeeAmount=escrow.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
    NodeIdentifier contractID = mockNodes.server1.getLocalNodeId();
    settlementClient.openPaymentChannel(contractID, escrowPlusFeeAmount);
    assertEquals(escrow, settlementClient.getAmountRemainingInChannel(contractID));
    Coin expectedBalance=escrow;
    Coin microPaymentAmount=Coin.CENT;
    for(int i=0; i<5; i++){
      assertEquals(expectedBalance, settlementClient.getAmountRemainingInChannel(contractID));
      settlementClient.makeMicroPayment(contractID, P2PBlobApplication.APP_ID, microPaymentAmount);
      expectedBalance=expectedBalance.subtract(microPaymentAmount);
    }
    settlementClient.closePaymentChannel(contractID);

    Coin expectedNewBalance=Coin.parseCoin(Double.toString(clientStartAmount))
        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to create the contract
        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to get the refund
        .subtract(Coin.valueOf(0, 5));  //The micro payments sum
    assertEquals(expectedNewBalance, settlementClient.clientKit.wallet().getBalance(BalanceType.ESTIMATED));
    
//    clientSettlementDB.getBalanceOfNode(mockNodes.server1.getLocalNodeId());
    settlementClient.close();
    clientSettlementDB.close();
    clientToServerConnection.close();
  }

}
