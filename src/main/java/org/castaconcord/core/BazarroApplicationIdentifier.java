package org.castaconcord.core;

import java.util.Arrays;


public class BazarroApplicationIdentifier extends BazarroIdentifier {
	public BazarroApplicationIdentifier(byte[] appIdBytes) {
		super(appIdBytes);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(binaryValue);
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
		BazarroApplicationIdentifier other = (BazarroApplicationIdentifier) obj;
		if (!Arrays.equals(binaryValue, other.binaryValue))
			return false;
		return true;
	}
}
