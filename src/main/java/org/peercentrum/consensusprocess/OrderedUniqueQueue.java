package org.peercentrum.consensusprocess;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

public class OrderedUniqueQueue<T> {
	LinkedHashSet<T> txQueue=new LinkedHashSet<T>();
	
	synchronized public void enqueueTransaction(T tx){
		txQueue.add(tx);
	}
	
	synchronized public List<T> dequeueAvailableTransactions(int nbTxRequired){
		LinkedList<T> availableTx = new LinkedList<T>();
		
		Iterator<T> queueIt = txQueue.iterator();
		int i=0;
		while(i<nbTxRequired && queueIt.hasNext()){
			availableTx.add(queueIt.next());
			queueIt.remove();
			i++;
		}
		return availableTx;
	}
}
