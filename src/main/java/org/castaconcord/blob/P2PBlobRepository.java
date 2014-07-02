package org.castaconcord.blob;

import org.castaconcord.h2pk.HashIdentifier;

abstract public class P2PBlobRepository {
	abstract public P2PBlobStoredBlob getStoredBlob(HashIdentifier blobId);
}
