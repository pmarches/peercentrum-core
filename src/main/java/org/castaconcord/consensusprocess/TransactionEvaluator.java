package org.castaconcord.consensusprocess;

public interface TransactionEvaluator<T extends ConsensusTransaction> {
	public boolean isTransactionValid(T tx);
}
