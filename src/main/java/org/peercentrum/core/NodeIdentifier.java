package org.peercentrum.core;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.peercentrum.h2pk.PublicKeyIdentifier;


public class NodeIdentifier extends PublicKeyIdentifier {
	public NodeIdentifier(String humanReadableIdentifier){
		super(humanReadableIdentifier);
	}
	
	public NodeIdentifier(byte[] binaryIdentifier){
		super(binaryIdentifier);
		if(binaryIdentifier==null || binaryIdentifier.length==0){
			throw new RuntimeException("invalid identifier");
		}
	}

  public NodeIdentifier(BCECPublicKey publicKeyOnCertificate) {
    this(publicKeyOnCertificate.getQ().getEncoded(true));
  }
}
