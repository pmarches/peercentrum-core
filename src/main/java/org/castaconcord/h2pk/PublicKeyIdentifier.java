package org.castaconcord.h2pk;

import org.castaconcord.core.Identifier;

public class PublicKeyIdentifier extends Identifier {

	public PublicKeyIdentifier(byte[] pkBytes) {
		super(pkBytes);
	}

	public PublicKeyIdentifier() {
		super();
	}

}
