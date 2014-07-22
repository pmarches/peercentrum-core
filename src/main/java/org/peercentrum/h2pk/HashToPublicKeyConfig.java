package org.peercentrum.h2pk;

import java.util.List;

public class HashToPublicKeyConfig {
	public List<String> validatorIdentifiers;
	
	@Override
	public String toString() {
		return "HashToPublicKeyConfig [validatorIdentifiers=" + validatorIdentifiers
				+ "]";
	}	
}
