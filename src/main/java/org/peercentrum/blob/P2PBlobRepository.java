package org.peercentrum.blob;

import org.peercentrum.h2pk.HashIdentifier;

abstract public class P2PBlobRepository {
	abstract public P2PBlobStoredBlob getStoredBlob(HashIdentifier blobId) throws Exception;
  abstract public P2PBlobStoredBlob createStoredBlob(P2PBlobHashList hashList, long blobLength, int blockSize) throws Exception;
}
