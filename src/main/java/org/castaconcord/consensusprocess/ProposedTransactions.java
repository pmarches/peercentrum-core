package org.castaconcord.consensusprocess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.castaconcord.core.BazarroNodeIdentifier;

import com.google.common.primitives.UnsignedLong;

public class ProposedTransactions<T extends ConsensusTransaction> implements Serializable, Iterable<T> {
	private static final long serialVersionUID = 7066902725948459145L;

	protected List<T> proposedTx = new ArrayList<T>();
	protected BazarroNodeIdentifier from;
	protected UnsignedLong applicableToDBVersionNumber;
	
	public ProposedTransactions(UnsignedLong applicableToLedgerNumber, BazarroNodeIdentifier from) {
		if(applicableToLedgerNumber==null){
			throw new NullPointerException();
		}
		this.applicableToDBVersionNumber = applicableToLedgerNumber;
		this.from = from;
	}
	
	public void addTransactions(T tx){
		proposedTx.add(tx);
	}

	@Override
	public Iterator<T> iterator() {
		return proposedTx.iterator();
	}

	public BazarroNodeIdentifier getFrom() {
		return from;
	}

	public int size() {
		return proposedTx.size();
	}

	@Override
	public String toString() {
		return "ProposedTransactions [applicableToLedgerNumber="
				+ applicableToDBVersionNumber + ", from=" + from + ", proposedTx="
				+ proposedTx + "]";
	}

	public int getDbVersionNumber() {
		return this.applicableToDBVersionNumber.intValue();
	}

}
