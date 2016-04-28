package org.peercentrum.h2pk;

import org.peercentrum.core.Identifier;

public class HashIdentifier extends Identifier {

	public HashIdentifier() {
		super();
	}
	
	public HashIdentifier(byte[] byteArray) {
		super(byteArray);
	}

  public HashIdentifier(String hexHashString) {
    super(hexHashString);
  }

}
