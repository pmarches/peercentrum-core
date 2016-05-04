package org.peercentrum.settlement;

import static org.junit.Assert.assertEquals;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet.BalanceType;
import org.junit.Test;
import org.peercentrum.TransientMockNetworkOfNodes;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.network.NetworkClientTCPConnection;

public class SettlementApplicationTest {

//  @Test
//  public void testPayNode() throws Exception {
//    TransientMockNetworkOfNodes mockNodes=new TransientMockNetworkOfNodes();
//    NetworkClientTCPConnection clientToServerConnection = mockNodes.networkClient1.createConnectionToPeer(mockNodes.server1.getLocalIdentifier());
//    double clientStartAmount=13.0;
//    mockNodes.fundBitcoinWalletOfNode(mockNodes.settlementClient1.clientKit.wallet(), clientStartAmount);
//
//    Coin escrow = Coin.valueOf(0, 10);
//    Coin escrowPlusFeeAmount=escrow.add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
//    NodeIdentifier contractID = mockNodes.server1.getLocalIdentifier();
//    mockNodes.settlementClient1.openPaymentChannel(contractID, escrowPlusFeeAmount);
//    assertEquals(escrow, mockNodes.settlementClient1.getAmountRemainingInChannel(contractID));
//    Coin expectedBalance=escrow;
//    Coin microPaymentAmount=Coin.CENT;
//    for(int i=0; i<5; i++){
//      assertEquals(expectedBalance, mockNodes.settlementClient1.getAmountRemainingInChannel(contractID));
//      mockNodes.settlementClient1.makeMicroPayment(P2PBlobApplication.APP_ID, microPaymentAmount);
//      expectedBalance=expectedBalance.subtract(microPaymentAmount);
//    }
//    mockNodes.settlementClient1.closePaymentChannel(contractID);
//
//    Coin expectedNewBalance=Coin.parseCoin(Double.toString(clientStartAmount))
//        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to create the contract
//        .subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE)  //The fee to get the refund
//        .subtract(Coin.valueOf(0, 5));  //The micro payments sum
//    assertEquals(expectedNewBalance, mockNodes.settlementClient1.clientKit.wallet().getBalance(BalanceType.ESTIMATED));
//    
////    clientSettlementDB.getBalanceOfNode(mockNodes.server1.getLocalNodeId());
//    mockNodes.shutdown();
//    clientToServerConnection.close();
//  }

}
