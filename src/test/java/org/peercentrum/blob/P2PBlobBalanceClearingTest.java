package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.peercentrum.core.NodeIdentifier;

public class P2PBlobBalanceClearingTest {
	@Test
	public void testPayment() throws Exception {
//		RippleWallet rippleWallet=new RippleWallet();
		P2PBlobTransferBalanceTable accounting=new P2PBlobTransferBalanceTable(null);
		accounting.setDefaultLoanAllowed(0);

		NodeIdentifier goodNodeId=new NodeIdentifier("some node that will pay us 10".getBytes());
		assertEquals(0, accounting.getBalanceForNode(goodNodeId));

		long costOfOperation=1;
		assertFalse(accounting.maybeDebit(goodNodeId, costOfOperation));
		accounting.creditNode(goodNodeId, 10);
		assertEquals(10, accounting.getBalanceForNode(goodNodeId));
		assertTrue(accounting.maybeDebit(goodNodeId, costOfOperation));
		assertEquals(9, accounting.getBalanceForNode(goodNodeId));

		NodeIdentifier leecherNodeId=new NodeIdentifier("some node that does not pay".getBytes());
		assertFalse(accounting.maybeDebit(leecherNodeId, 1));
		accounting.setDefaultLoanAllowed(1);
		assertTrue(accounting.maybeDebit(leecherNodeId, 1));
	}
	
	@Test
	public void testThreading() throws Exception{
		Logger.getLogger(P2PBlobTransferBalanceTable.class).setLevel(Level.OFF); //This test is way too verbose
		final P2PBlobTransferBalanceTable accounting=new P2PBlobTransferBalanceTable(null);
		final NodeIdentifier goodNodeId=new NodeIdentifier("some node that will pay us 10".getBytes());
		final int NB_THREADS=10;
		final CountDownLatch jobsReady=new CountDownLatch(NB_THREADS);
		final CountDownLatch jobsDone=new CountDownLatch(NB_THREADS);
		Runnable updates=new Runnable() {
			@Override
			public void run() {
				try {
					jobsReady.countDown();
					jobsReady.await();
					for(int i=0; i<50; i++){
						accounting.creditNode(goodNodeId, 1);
						accounting.maybeDebit(goodNodeId, 1);
						Thread.yield();
						accounting.getBalanceForNode(goodNodeId);
					}
					jobsDone.countDown();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};

		for(int t=0; t<10; t++){
			new Thread(updates).start();
		}
		try {
			jobsDone.await();
			assertEquals(0, accounting.getBalanceForNode(goodNodeId));
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
