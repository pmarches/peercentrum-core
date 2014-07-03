package org.peercentrum.consensusprocess;

import org.peercentrum.consensusprocess.ConsensusDB;
import org.peercentrum.consensusprocess.ConsensusProcess;
import org.peercentrum.consensusprocess.ProposedTransactions;
import org.peercentrum.consensusprocess.TransactionEvaluator;
import org.peercentrum.consensusprocess.UniqueNodeList;
import org.peercentrum.core.NodeIdentifier;

public class MockConsensusProcess extends ConsensusProcess {
	public MockConsensusProcess(NodeIdentifier thisNode, TransactionEvaluator txEvaluator, UniqueNodeList unl, ConsensusDB db) {
		super(thisNode, txEvaluator, unl, db, null);
	}

	@Override
	protected void broadcastProposedTransactions(ProposedTransactions ourProposedTx) {
	}

}
