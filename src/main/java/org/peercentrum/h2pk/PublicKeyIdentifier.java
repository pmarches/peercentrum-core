package org.peercentrum.h2pk;

import javax.xml.bind.DatatypeConverter;

import org.peercentrum.core.Identifier;

public class PublicKeyIdentifier extends Identifier {

  public PublicKeyIdentifier(byte[] pkBytes) {
		super(pkBytes);
	}

	public PublicKeyIdentifier() {
		super();
	}

  public PublicKeyIdentifier(String humanReadableIdentifier) {
    super(humanReadableIdentifier);
  }

  @Override
  public String toString() {
    return DatatypeConverter.printHexBinary(binaryValue);
  }
}
