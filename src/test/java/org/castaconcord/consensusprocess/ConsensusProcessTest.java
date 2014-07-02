package org.castaconcord.consensusprocess;

import static org.junit.Assert.assertEquals;

import org.castaconcord.core.NodeIdentifier;
import org.junit.Test;

import com.google.common.primitives.UnsignedLong;

public class ConsensusProcessTest extends BaseTestCase {
	TransactionEvaluator txEvaluator=new TransactionEvaluator() {
		public boolean isTransactionValid(ConsensusTransaction tx) {
			MockTransaction mtx = (MockTransaction) tx;
			return mtx.isValidTransaction;
		}
	};

	@Test
	public void testProcessor() {
		NodeIdentifier thisNode=new NodeIdentifier("ThisNode");
		ConsensusDB db=null;
		ConsensusProcess proc = new MockConsensusProcess(thisNode, txEvaluator, unl, db);
		proc.moveLocalTransactionsToCandidateSet();
		
		ProposedTransactions txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(0, txReadyToBePublished.size());
		
		proc.receiveLocalTransaction(tx1);
		proc.receiveLocalTransaction(tx7_invalid);
		proc.moveLocalTransactionsToCandidateSet();
		proc.receiveProposedTransactions(nodeZProposal_1_2_3);

		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(1, txReadyToBePublished.size());
		assertEquals(thisNode, txReadyToBePublished.getFrom());

		proc.receiveProposedTransactions(nodeAProposal_1_3);
		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(1, txReadyToBePublished.size());

		proc.receiveProposedTransactions(nodeCProposal_1_3);
		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(2, txReadyToBePublished.size());

		proc.consensusThreshold.incrementProposalThreshold(); //60%
		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(1, txReadyToBePublished.size());

		proc.consensusThreshold.incrementProposalThreshold(); //70%
		
		proc.receiveProposedTransactions(nodeAProposal_1_3);
		proc.receiveProposedTransactions(nodeBProposal_2);
		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(1, txReadyToBePublished.size());

		proc.consensusThreshold.incrementProposalThreshold(); //80%

		proc.receiveProposedTransactions(nodeBProposal_1_2_3);
		proc.receiveProposedTransactions(nodeCProposal_1_3);
		proc.receiveProposedTransactions(nodeDProposal_1_3);
		txReadyToBePublished = proc.candidateTXSet.packageProposals(thisNode, UnsignedLong.ONE, 1);
		assertEquals(2, txReadyToBePublished.size());
	}

}
