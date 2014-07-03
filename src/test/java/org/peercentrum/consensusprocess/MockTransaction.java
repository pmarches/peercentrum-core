package org.peercentrum.consensusprocess;

import org.peercentrum.consensusprocess.ConsensusTransaction;



public class MockTransaction extends ConsensusTransaction {
	private static final long serialVersionUID = 6755003851010057150L;
	public boolean isValidTransaction;
	public int txId;
	public MockTransaction(boolean isValidTransaction, int txId){
		this.isValidTransaction = isValidTransaction;
		this.txId=txId;
	}

	@Override
	public String toString() {
		return Integer.toString(txId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isValidTransaction ? 1231 : 1237);
		result = prime * result + txId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MockTransaction other = (MockTransaction) obj;
		if (isValidTransaction != other.isValidTransaction)
			return false;
		if (txId != other.txId)
			return false;
		return true;
	}
	
	
}
