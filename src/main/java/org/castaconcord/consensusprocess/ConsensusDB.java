package org.castaconcord.consensusprocess;

import com.google.common.primitives.UnsignedLong;

public abstract class ConsensusDB<T extends ConsensusTransaction> {
	protected UnsignedLong dbVersion;
	public Object newVersionMonitor=new Object();

	public ConsensusDB(UnsignedLong dbVersion) {
		this.dbVersion=dbVersion;
	}
	
	public UnsignedLong getDBVersionNumber() {
		return dbVersion;
	}

	public UnsignedLong incrementVersion() {
		dbVersion=dbVersion.plus(UnsignedLong.ONE);
		synchronized (newVersionMonitor) {
			newVersionMonitor.notifyAll();
		}
		return dbVersion;
	}
	
	public void awaitForVersion(int desiredVersion) {
		synchronized(newVersionMonitor){
			try {
				while(dbVersion.intValue()<desiredVersion){
					newVersionMonitor.wait();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void integrateProposedTransactions(ProposedTransactions<? extends ConsensusTransaction> validTX) throws Exception {
		for(ConsensusTransaction oneTx : validTX){
			applyOneTransaction((T) oneTx);
		}
	}

	abstract public void applyOneTransaction(T tx) throws Exception;
}
