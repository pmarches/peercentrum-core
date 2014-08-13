package org.peercentrum.h2pk;

import javax.xml.bind.DatatypeConverter;

import org.peercentrum.core.Identifier;

import com.google.bitcoin.core.ECKey;

public class PublicKeyIdentifier extends Identifier {

	private ECKey ecKey;

  public PublicKeyIdentifier(byte[] pkBytes) {
		super(pkBytes);
    this.ecKey=ECKey.fromPublicOnly(binaryValue);
	}

	public PublicKeyIdentifier() {
		super();
    this.ecKey=ECKey.fromPublicOnly(binaryValue);
	}

  public PublicKeyIdentifier(String humanReadableIdentifier) {
    super(humanReadableIdentifier);
    this.ecKey=ECKey.fromPublicOnly(binaryValue);
  }

  @Override
  public String toString() {
    return DatatypeConverter.printHexBinary(ecKey.getPubKeyPoint().getEncoded(true));
  }
}
