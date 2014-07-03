package org.peercentrum.consensusprocess;

import java.util.HashMap;
import java.util.Iterator;

import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;


public class CandidateTransactionSet {
	private static final Logger LOGGER = LoggerFactory.getLogger(CandidateTransactionSet.class);

	protected HashMap<ConsensusTransaction, HashMap<NodeIdentifier, Boolean>> votesPerTX = new HashMap<>();
	
	public ProposedTransactions<?> packageProposals(NodeIdentifier thisNode, UnsignedLong DBVersionNumber, int nbValidatorRequired){
		LOGGER.trace("votesPerTX.size={}", votesPerTX.size());
		LOGGER.trace("votesPerTX={}", votesPerTX);

		ProposedTransactions<? super ConsensusTransaction> newProposal=new ProposedTransactions<>(DBVersionNumber, thisNode);
		Iterator<ConsensusTransaction> txIt=getTransactionIterator();
		while(txIt.hasNext()){
			ConsensusTransaction currentTx = txIt.next();
			HashMap<NodeIdentifier, Boolean> txVotes = votesPerTX.get(currentTx);
			int nbValidatorsThatAcceptedTx=0;
			for(Boolean oneVote : txVotes.values()){
				if(oneVote.equals(Boolean.TRUE)){
					nbValidatorsThatAcceptedTx++;
				}
			}
			if(nbValidatorsThatAcceptedTx>= nbValidatorRequired){
				newProposal.addTransactions(currentTx);
			}
			else{
				LOGGER.debug("TX {} has not reached consensus. nbValidatorsThatAcceptedTx={} nbValidatorRequired={}", 
						new Object[]{currentTx, nbValidatorsThatAcceptedTx, nbValidatorRequired});
			}
		}
		LOGGER.debug("packaged newProposal={}", newProposal);
		return newProposal;
	}
	
//	public boolean hasConsensusBeenReached(){
//		int nbApprovedTransactions=0;
//		Iterator<Transaction> txIt=getTransactionIterator();
//		while(txIt.hasNext()){
//			Hashtable<NodeIdentifier, Boolean> nodesThatApprovedThisTx = votesPerTX.get(txIt.next());
//			final int nbNodesRequiredForConsensus=(int) (this.validatorCount*CONSENSUS_THRESHOLD);
//			if(nodesThatApprovedThisTx.size()>=nbNodesRequiredForConsensus){
//				nbApprovedTransactions++;
//			}
//		}
//		return false;
//	}
	
	private Iterator<ConsensusTransaction> getTransactionIterator() {
		return votesPerTX.keySet().iterator();
	}

	synchronized public void setApproval(NodeIdentifier from, ConsensusTransaction tx, boolean isTransactionApprovedByNode) {
		HashMap<NodeIdentifier, Boolean> nodeApprovalsOfThisTx = votesPerTX.get(tx);
		if(nodeApprovalsOfThisTx==null){
			nodeApprovalsOfThisTx = new HashMap<NodeIdentifier, Boolean>(); 
			votesPerTX.put(tx, nodeApprovalsOfThisTx);
		}
		Boolean otherNodeVoteOnTX=nodeApprovalsOfThisTx.get(from);
		if(otherNodeVoteOnTX==null){
			nodeApprovalsOfThisTx.put(from, isTransactionApprovedByNode);
		}
		else if(otherNodeVoteOnTX.equals(isTransactionApprovedByNode)==false){
			LOGGER.warn("Node {} changed it's mind on TX {}?", from, tx);
			nodeApprovalsOfThisTx.put(from, isTransactionApprovedByNode);
		}
		LOGGER.debug("approval set as votesPerTX={}", votesPerTX);
	}
	
	public void removeApprovedTransactions(ProposedTransactions<ConsensusTransaction> ourProposedTx){
		for(ConsensusTransaction tx : ourProposedTx.proposedTx){
			votesPerTX.remove(tx);
		}
	}

	public int size() {
		return votesPerTX.size();
	}

}
