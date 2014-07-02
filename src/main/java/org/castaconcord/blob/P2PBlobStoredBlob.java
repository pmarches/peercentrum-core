package org.castaconcord.blob;

import java.security.InvalidParameterException;

import org.castaconcord.h2pk.HashIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.protobuf.ByteString;

public abstract class P2PBlobStoredBlob {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobStoredBlob.class);
	
	protected HashIdentifier blobHash;
	protected P2PBlobRangeSet localBlockRange;
	protected P2PBlobHashList hashList;
	protected long blobLengthInBytes;

	abstract protected void acceptValidatedBlobBytes(int blockIndex, byte[] blobBlockBytes);
	abstract public ByteString getBytesRange(long offset, int length) throws Exception;

	public P2PBlobStoredBlob(HashIdentifier blobHash, P2PBlobHashList hashList, P2PBlobRangeSet localBlockRange, long blobByteSize) {
		this.blobHash=blobHash;
		this.hashList=hashList;
		this.localBlockRange=localBlockRange;
		this.blobLengthInBytes=blobByteSize;
	}
	
	public boolean isBlobDownloadComplete() {
		if(hashList==null){
			return false; //We need the hashList to determine if the download has been completed
		}
		if(localBlockRange==null){
			return true;
		}
		final Range<Integer> fullRange = Range.<Integer>closed(0, hashList.size()-1);
		return localBlockRange.ranges.encloses(fullRange);
	}

	public void setHashList(P2PBlobHashList hashList) {
		if(hashList.getTopLevelHash().equals(blobHash)==false){
			throw new InvalidParameterException("The hashList "+hashList.getTopLevelHash()+" does not compute to the blobHash "+blobHash);
		}
		this.hashList=hashList;
	}
	public P2PBlobHashList getHashList() {
		return hashList;
	}

	public P2PBlobRangeSet getMissingRanges() {
		if(hashList==null){
			throw new NullPointerException("Need to have the blocks hashList before computing the missing blocks");
		}
		if(localBlockRange==null){
			LOGGER.warn("localBlockRange is null, meaning the download is complete");
			return null;
		}
		final Range<Integer> fullRange = Range.<Integer>closed(0, hashList.size()-1);
		RangeSet<Integer> missingRanges=TreeRangeSet.create();
		missingRanges.add(fullRange);
		missingRanges.removeAll(localBlockRange.ranges);
		return new P2PBlobRangeSet(missingRanges);
	}

	public HashIdentifier getBlobIdentifier() {
		return blobHash;
	}
	
	public long getBlobLength() {
		return blobLengthInBytes;
	}

}
