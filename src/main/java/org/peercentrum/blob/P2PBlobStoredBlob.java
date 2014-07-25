package org.peercentrum.blob;

import java.security.InvalidParameterException;

import org.peercentrum.core.PB;
import org.peercentrum.h2pk.HashIdentifier;
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
	protected int blockLength=P2PBlobApplication.BLOCK_SIZE;
	
	abstract protected void acceptValidatedBlobBytes(int blockIndex, byte[] blobBlockBytes) throws Exception;
	abstract public ByteString getBytesRange(long offset, int length) throws Exception;

	public P2PBlobStoredBlob(HashIdentifier blobHash, P2PBlobHashList hashList, P2PBlobRangeSet localBlockRange, long blobByteSize, int blockLength) {
		this.blobHash=blobHash;
		this.hashList=hashList;
		this.localBlockRange=localBlockRange;
		this.blobLengthInBytes=blobByteSize;
		this.blockLength=blockLength;
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
	  if(hasMetaData()==false){
      throw new NullPointerException("The metadata has to be known in order to determine the BlobLength");
	  }
		return blobLengthInBytes;
	}

	public boolean hasMetaData() {
    return hashList!=null && blobLengthInBytes!=-1;
  }

	public int getLengthInBytesOfLastBlock() {
	  if(hasMetaData()==false){
	    throw new NullPointerException("The metadata has to be known in order to determine the LengthInBytesOfLastBlock");
	  }
    return (int) (blobLengthInBytes%blockLength);
  }
	
  public void setMetaData(PB.P2PBlobMetaDataMsg metaData) throws Exception {
    if(hashList!=null){
      LOGGER.warn("We already have the hashList, no need to setMetaData twice..");
      return;
    }
    if(metaData.hasHashList()==false || metaData.hasBlobLength()==false){
      throw new Exception("metadata '"+metaData+"' is missing fields : ");
    }

    P2PBlobHashList newHashList = new P2PBlobHashList(metaData.getHashList());
    if(newHashList.getTopLevelHash().equals(blobHash)==false){
      throw new InvalidParameterException("The hashList "+hashList.getTopLevelHash()+" does not compute to the blobHash "+blobHash);
    }
    this.hashList=newHashList;
    this.blobLengthInBytes=metaData.getBlobLength();
    if(metaData.hasBlockSize()){
      this.blockLength=metaData.getBlockSize();
    }
  }

  public P2PBlobRangeSet getLocalRange() {
    return new P2PBlobRangeSet(0, hashList.size()-1);
  }

  public long getNumberOfBytesInBlockRange(P2PBlobRangeSet ranges) {
    int numberOfFullBlocks=ranges.getCardinality();
    int numberOfBytesInLastBlock=0;
    if(ranges.ranges.contains(hashList.size()-1)){
      numberOfBytesInLastBlock=getLengthInBytesOfLastBlock();
      if(numberOfBytesInLastBlock!=0){
        numberOfFullBlocks--;
      }
    }
    return numberOfFullBlocks*blockLength+numberOfBytesInLastBlock;
  }

  public void maybeAcceptBlobBytes(PB.P2PBlobBlockMsg blobBlockMsg) throws Exception {
    if(this.hashList==null){
      throw new RuntimeException("Need to receive the hashList before accepting blocks");
    }
    if(blobBlockMsg.hasBlockIndex()==false || blobBlockMsg.hasBlobBytes()==false){
      throw new RuntimeException("Missing blockIndex or blockBytes");
    }
    int currentBlockIndex=blobBlockMsg.getBlockIndex();
    byte[] blobBlockBytes=blobBlockMsg.getBlobBytes().toByteArray();
    
    HashIdentifier blockHash=this.hashList.get(currentBlockIndex);
    HashIdentifier blobBlockBytesHash=P2PBlobHashList.hashBytes(blobBlockBytes, 0 ,blobBlockBytes.length);
    if(blobBlockBytesHash.equals(blockHash)==false){
      LOGGER.error("The block "+currentBlockIndex+" does not hash to "+blockHash);
      return; //Throw exception?
    }
    this.acceptValidatedBlobBytes(currentBlockIndex, blobBlockBytes);
  }
  
  public int getBlockLength() {
    return this.blockLength;
  }
  public int getNumberOfBlocks() {
    return getHashList().size();
  }

}
