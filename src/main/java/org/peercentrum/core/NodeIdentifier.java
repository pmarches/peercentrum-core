package org.peercentrum.core;

import org.peercentrum.h2pk.PublicKeyIdentifier;


public class NodeIdentifier extends PublicKeyIdentifier {
	public NodeIdentifier(String humanReadableIdentifier){
		super(humanReadableIdentifier.getBytes());
	}
	
	public NodeIdentifier(byte[] binaryIdentifier){
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
