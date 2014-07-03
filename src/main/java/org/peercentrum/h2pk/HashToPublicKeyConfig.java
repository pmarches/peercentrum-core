package org.peercentrum.h2pk;

import java.util.List;

public class HashToPublicKeyConfig {
	String databasePath;
	List<String> validatorIdentifiers;

	public String getDatabasePath() {
		return databasePath;
	}
	public void setDatabasePath(String databasePath) {
		this.databasePath = databasePath;
	}
	public List<String> getValidatorIdentifiers() {
		return validatorIdentifiers;
	}
	public void setValidatorIdentifiers(List<String> validatorIdentifiers) {
		this.validatorIdentifiers = validatorIdentifiers;
	}
	
	@Override
	public String toString() {
		return "HashToPublicKeyConfig [databasePath=" + databasePath + ", validatorIdentifiers=" + validatorIdentifiers
				+ "]";
	}	
}
