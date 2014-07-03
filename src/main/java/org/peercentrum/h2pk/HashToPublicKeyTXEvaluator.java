package org.peercentrum.h2pk;

import org.peercentrum.consensusprocess.TransactionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HashToPublicKeyTXEvaluator implements TransactionEvaluator<HashToPublicKeyTransaction> {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashToPublicKeyTXEvaluator.class);
	
	HashToPublicKeyDB db;

	public HashToPublicKeyTXEvaluator(HashToPublicKeyDB db) {
		this.db=db;
	}
	
	public boolean isSignatureValid(HashToPublicKeyTransaction tx){
		/*
		We need to generate some data that will be signed by the key owner. This data needs to be
		different for each add/remove operation otherwise a replay attack is possible
		If we use the databaseVersionNumber it is possible the transaction may arrive after the
		database has been versioned. Maybe the transaction should state the future version number 
		it wishes to be included in. This ensures the TX will be included in a specific version.
		Another possiblity, is to specify an upper bound version number. The TX may be integrated
		only up to this version number. This allows replay attacks to occur until that upper bound 
		has been reached, but it's effects may be eliminated.	 
		*/

		//FIXME implement signature scheme
		return tx.signature!=null && tx.signature.getBytes().length!=0;
	}

	/**
	 * Determines if the TX can be applied to the database. In this case, if we have a Append() operation
	 * we need 
	 * 	- to have an existing PK associated to the address.
	 *  - Remaining space in the address
	 *  - Other stuff..
	 * 
	 * @param tx
	 * @return
	 */
	@Override
	public boolean isTransactionValid(HashToPublicKeyTransaction tx) {
		boolean pkAlreadyRegistered = db.isPublicKeyRegisteredForAddress(tx.getAddress(), tx.getPublicKey());
		if(tx.isAppend()){
			if(pkAlreadyRegistered){
				return false;
			}
		}
		else if(tx.isRemove()){
			if(pkAlreadyRegistered==false){
				return false;
			}
		}
		else{
			LOGGER.error("unsupported operation "+tx);
			return false;
		}
		return true;
	}
}
