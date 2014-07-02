package org.castaconcord.consensusprocess;

import org.castaconcord.core.NodeIdentifier;

public class MockConsensusProcess extends ConsensusProcess {
	public MockConsensusProcess(NodeIdentifier thisNode, TransactionEvaluator txEvaluator, UniqueNodeList unl, ConsensusDB db) {
		super(thisNode, txEvaluator, unl, db, null);
	}

	@Override
	protected void broadcastProposedTransactions(ProposedTransactions ourProposedTx) {
	}

}
