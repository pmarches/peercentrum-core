package org.peercentrum.h2pk;

import org.peercentrum.core.Identifier;

public class PublicKeyIdentifier extends Identifier {

	public PublicKeyIdentifier(byte[] pkBytes) {
		super(pkBytes);
	}

	public PublicKeyIdentifier() {
		super();
	}

}
