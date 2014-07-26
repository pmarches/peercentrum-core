package org.peercentrum.blob;

public class P2PBlobBlockLayout {
  final long blobLengthInBytes;
  final int blockLength;
  
  public P2PBlobBlockLayout(long blobLengthInBytes, int blockLength) {
    this.blobLengthInBytes=blobLengthInBytes;
    this.blockLength=blockLength;
  }
  
  public int getBlockLengthOfLastBlock() {
    return (int) (blobLengthInBytes%blockLength);
  }

  public int getBlockLength() {
    return blockLength;
  }

  public long getBlobLength() {
    return blobLengthInBytes;
  }

  public int getNumberOfBlocks() {
    int nbBlocks=(int) (blobLengthInBytes/blockLength);
    if(getBlockLengthOfLastBlock()!=0){
      nbBlocks++;
    }
    return nbBlocks;
  }

  public int getBlockLength(int blockIndex) {
    if(blockIndex+1==getNumberOfBlocks()){
      return getBlockLengthOfLastBlock();
    }
    return getBlockLength();
  }

  public long getBlockOffset(int blockIndex) {
    return blockIndex*getBlockLength();
  }

}
