package org.peercentrum.h2pk;

import java.security.PublicKey;

import javax.xml.bind.DatatypeConverter;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.peercentrum.core.Identifier;
import org.peercentrum.network.NodeIdentity;

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

  public PublicKey getPublicKey() {
    ECPoint ecPoint=NodeIdentity.secp256k1.getCurve().decodePoint(binaryValue);
    ECPublicKeySpec spec=new ECPublicKeySpec(ecPoint, NodeIdentity.secp256k1);
    return new BCECPublicKey("EC", spec, null);
  }

  @Override
  public String toString() {
    return DatatypeConverter.printHexBinary(binaryValue);
  }
}
