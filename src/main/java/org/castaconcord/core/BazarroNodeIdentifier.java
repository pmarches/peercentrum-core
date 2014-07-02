package org.castaconcord.core;

import org.castaconcord.h2pk.BazarroPublicKeyIdentifier;


public class BazarroNodeIdentifier extends BazarroPublicKeyIdentifier {
	public BazarroNodeIdentifier(String humanReadableIdentifier){
		super(humanReadableIdentifier.getBytes());
	}
	
	public BazarroNodeIdentifier(byte[] binaryIdentifier){
		super(binaryIdentifier);
		if(binaryIdentifier==null || binaryIdentifier.length==0){
			throw new RuntimeException("invalid identifier");
		}
	}
	
	@Override
	public String toString() {
		return new String(binaryValue);
	}
}
