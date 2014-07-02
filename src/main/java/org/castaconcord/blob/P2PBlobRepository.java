package org.castaconcord.blob;

import org.castaconcord.h2pk.BazarroHashIdentifier;

abstract public class P2PBlobRepository {
	abstract public P2PBlobStoredBlob getStoredBlob(BazarroHashIdentifier blobId);
}
