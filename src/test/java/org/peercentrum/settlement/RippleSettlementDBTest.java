package org.peercentrum.settlement;
//package org.peercentrum.settlement;
//
//import static org.junit.Assert.assertEquals;
//
//import java.io.File;
//import java.math.BigInteger;
//
//import org.peercentrum.core.NodeIdentifier;
//import org.junit.Test;
//
//public class RippleSettlementDBTest {
//	static final NodeIdentifier node1=new NodeIdentifier("node1");
//	static final NodeIdentifier node2=new NodeIdentifier("node2");
//
//	@Test
//	public void testUpdateBalancesWithNewTransactionsFromNetwork() throws Exception {
//		RippleWallet node1Wallet=new RippleWallet(new File("node1.rippleWallet"));
//		RippleWallet node2Wallet=new RippleWallet(new File("node2.rippleWallet"));
//
//		RippleAddress node1Address=node1Wallet.getSeed().getPublicRippleAddress();
//		RippleAddress node2Address=node2Wallet.getSeed().getPublicRippleAddress();
//		System.out.println("node1="+node1Address);
//		System.out.println("node2="+node2Address);
//
//		RippleSettlementDB node1XRPDB = new RippleSettlementDB(node1Address, null);
//		node1XRPDB.setSettlementMethod(node2, node2Address);
//		node1XRPDB.startMonitoringTransactions();
//		assertEquals(0, node1XRPDB.getBalanceForNode(node1)); //Our own node
//		long previousBalance=node1XRPDB.getBalanceForNode(node2);
//
//		node2Wallet.sendXRP(BigInteger.ONE, node1Address);
//		Thread.sleep(20_000);
//		assertEquals(previousBalance+1, node1XRPDB.getBalanceForNode(node2));
//		node1XRPDB.close();
//	}
//
//}
