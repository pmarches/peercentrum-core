package org.castaconcord.h2pk;

import org.castaconcord.core.BazarroIdentifier;

public class BazarroPublicKeyIdentifier extends BazarroIdentifier {

	public BazarroPublicKeyIdentifier(byte[] pkBytes) {
		super(pkBytes);
	}

	public BazarroPublicKeyIdentifier() {
		super();
	}

}
