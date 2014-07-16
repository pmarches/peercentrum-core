package org.peercentrum.settlement;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.core.TransientMockNetworkOfNodes;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet.BalanceType;

public class SettlementApplicationTest {

  @Test
  public void testPayNode() throws Exception {
    TransientMockNetworkOfNodes mockNodes=new TransientMockNetworkOfNodes();
    SettlementDB clientSettlementDB=new SettlementDB(null);
    SettlementApplicationClient settlementClient=new SettlementApplicationClient(mockNodes.networkClient1, mockNodes.client1Config, clientSettlementDB.settlementMethod);
    double clientStartAmount=13.0;
    mockNodes.fundBitcoinWalletOfNode(settlementClient.clientKit.wallet(), clientStartAmount);

    Coin escrow = Coin.valueOf(0, 10);
    Coin escrowPlusFeeAmount=escrow.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
    settlementClient.openPaymentChannelTo(mockNodes.server1.getLocalNodeId(), escrowPlusFeeAmount);
    assertEquals(escrow, settlementClient.getAmountRemainingInChannel(mockNodes.server1.getLocalNodeId()));
    Coin expectedBalance=escrow;
    Coin microPaymentAmount=Coin.CENT;
    for(int i=0; i<5; i++){
      assertEquals(expectedBalance, settlementClient.getAmountRemainingInChannel(mockNodes.server1.getLocalNodeId()));
      settlementClient.makeMicroPayment(mockNodes.server1.getLocalNodeId(), P2PBlobApplication.APP_ID, microPaymentAmount);
      expectedBalance=expectedBalance.subtract(microPaymentAmount);
    }
    settlementClient.closePaymentChannel(mockNodes.server1.getLocalNodeId());

    Coin expectedNewBalance=Coin.parseCoin(Double.toString(clientStartAmount))
        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to create the contract
        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to get the refund
        .subtract(Coin.valueOf(0, 5));  //The micro payments sum
    assertEquals(expectedNewBalance, settlementClient.clientKit.wallet().getBalance(BalanceType.ESTIMATED));
    settlementClient.close();
    clientSettlementDB.close();
  }

}
