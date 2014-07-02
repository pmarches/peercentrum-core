package org.castaconcord.consensusprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.castaconcord.h2pk.HashIdentifier;
import org.castaconcord.h2pk.HashPointerSignature;
import org.castaconcord.h2pk.PublicKeyIdentifier;
import org.castaconcord.h2pk.HashToPublicKeyTransaction;
import org.junit.Test;

import com.google.common.primitives.UnsignedLong;


public class CandidateTransactionSetTest extends BaseTestCase {
	
	@Test
	public void test() throws CloneNotSupportedException {
		CandidateTransactionSet cs = new CandidateTransactionSet();

		cs.setApproval(nodeA, tx7_invalid, false);
		assertEquals(1, cs.votesPerTX.size());
		cs.setApproval(nodeA, tx6, true);
		assertEquals(2, cs.votesPerTX.size());
		
		ProposedTransactions prop1 = cs.packageProposals(nodeB, UnsignedLong.ONE, 1);
		assertEquals(1, prop1.proposedTx.size());
		
		cs.setApproval(nodeC, tx6_clone, true);
		assertEquals(2, cs.votesPerTX.size());
		
		HashToPublicKeyTransaction h2pkTx=new HashToPublicKeyTransaction(new HashIdentifier(), new PublicKeyIdentifier(), true, new HashPointerSignature());
		HashToPublicKeyTransaction h2pkTxClone=h2pkTx.clone();
		assertEquals(h2pkTx, h2pkTxClone);
		assertNotSame(h2pkTx, h2pkTxClone);
		
		cs.setApproval(nodeA, h2pkTx, true);
		cs.setApproval(nodeB, h2pkTxClone, true);
		assertEquals(3, cs.votesPerTX.size());
	}

}
