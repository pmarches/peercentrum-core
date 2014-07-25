package org.peercentrum.blob;

import org.peercentrum.core.PB.P2PBlobMetaDataMsg;
import org.peercentrum.h2pk.HashIdentifier;

abstract public class P2PBlobRepository {
	abstract public P2PBlobStoredBlob getStoredBlob(HashIdentifier blobId) throws Exception;
  abstract public P2PBlobStoredBlob getOrCreateStoredBlob(P2PBlobMetaDataMsg metaData) throws Exception;
}
