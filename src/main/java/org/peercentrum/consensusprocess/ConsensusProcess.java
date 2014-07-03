package org.peercentrum.consensusprocess;

import java.util.Iterator;

import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.primitives.UnsignedLong;

public abstract class ConsensusProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsensusProcess.class);
	
	protected CandidateTransactionSet candidateTXSet;
	protected TransactionQueue localValidTXQueue=new TransactionQueue();
	protected TransactionEvaluator<? super ConsensusTransaction> txEvaluator;
	public UniqueNodeList unl;
	protected NodeIdentifier localNodeIdentifier;
	protected ConsensusDB<? super ConsensusTransaction> consensusDB;
	public ConsensusThreshold consensusThreshold;

	public Runnable continuousDatabaseCloseProcess = new Runnable(){
		@Override
		public void run() {
			try {
				MDC.put("NodeId", localNodeIdentifier.toString());
				//TODO Sync up with the other nodes
				
				while(Thread.interrupted()==false){
					executeOneDBCloseCycle();
				}
				MDC.remove("NodeId");
			} catch (Exception e) {
				//FIXME Handle exception sanely
				e.printStackTrace();
			}
		}
	};

	public Runnable stepDatabaseCloseProcess=new Runnable() {
		@Override
		public void run() {
			try {
				MDC.put("NodeId", localNodeIdentifier.toString());
				executeOneDBCloseCycle();
				MDC.remove("NodeId");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	abstract protected void broadcastProposedTransactions(ProposedTransactions<? super ConsensusTransaction> ourProposedTx);

	public ConsensusProcess(NodeIdentifier thisNodeID,
			TransactionEvaluator txEvaluator,
			UniqueNodeList unl,
			ConsensusDB consensusDB,
			ConsensusThreshold consensusThreshold){
		this.localNodeIdentifier = thisNodeID;
		this.txEvaluator= txEvaluator;
		this.unl = unl;
		this.consensusDB=consensusDB;
		this.candidateTXSet = new CandidateTransactionSet();
		this.consensusThreshold=consensusThreshold;
	}
		
	public void receiveLocalTransaction(ConsensusTransaction localTx){
		//Check if the TX is coherent with the DB
		boolean txIsValid=txEvaluator.isTransactionValid(localTx);
		if(txIsValid){
			localValidTXQueue.enqueueTransaction(localTx);
		}
		else{
			LOGGER.warn("received a invalid local transaction "+localTx);
		}
	}
	
	public void receiveProposedTransactions(ProposedTransactions proposedTransaction){
		//Check if the TX is coherent with the DB
		if(!unl.isNodeInList(proposedTransaction.getFrom())){
			LOGGER.warn("We have received a proposed tx from node "+proposedTransaction.getFrom()+" that is not in our UNL, discarding");
			return;
		}
		LOGGER.debug("received proposed transactions of size {} from node {}", proposedTransaction.size(), proposedTransaction.getFrom());
		LOGGER.debug("before candidateTXSet contains {} transactions.", candidateTXSet.size());
		
		Iterator<ConsensusTransaction> it = proposedTransaction.iterator();
		while(it.hasNext()){
			ConsensusTransaction tx = it.next();
			candidateTXSet.setApproval(proposedTransaction.getFrom(), tx, true);
			boolean currentTxIsConsideredValidByLocalNode=txEvaluator.isTransactionValid(tx);
			if(currentTxIsConsideredValidByLocalNode){
				candidateTXSet.setApproval(localNodeIdentifier, tx, true);
			}
			else{
				LOGGER.warn("Node "+proposedTransaction.getFrom()+" is lying on tx "+tx+"?");
				//TODO Should we punish the node?
			}
		}
		LOGGER.debug("after candidateTXSet now contains {} transactions.", candidateTXSet.size());
	}
	
	protected void moveLocalTransactionsToCandidateSet() {
		LOGGER.debug("Moving local TX to candidate set");
		Iterator<ConsensusTransaction> it = this.localValidTXQueue.dequeueAvailableTransactions(100).iterator();
		while(it.hasNext()){
			candidateTXSet.setApproval(localNodeIdentifier, it.next(), true);
		}
		LOGGER.debug("candidateTXSet now contains {} transactions.", candidateTXSet.size());
	}

	public void executeOneDBCloseCycle() throws Exception {
		UnsignedLong dbVersionOfThisCycle=consensusDB.getDBVersionNumber();
		moveLocalTransactionsToCandidateSet();
		ProposedTransactions ourProposedTx=null;
		do{
			int nbValidatorRequired=consensusThreshold.getNbValidatorRequired(unl.size());
			ourProposedTx = candidateTXSet.packageProposals(localNodeIdentifier, dbVersionOfThisCycle, nbValidatorRequired);
			broadcastProposedTransactions(ourProposedTx);
		}
		while(consensusThreshold.awaitNextThresholdIncrement());
		LOGGER.debug("Close cycle reached, versionning DB");

		candidateTXSet.removeApprovedTransactions(ourProposedTx);
		consensusDB.incrementVersion();
		consensusDB.integrateProposedTransactions(ourProposedTx);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName()+" "+localNodeIdentifier.toString();
	}
}
