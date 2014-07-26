package org.peercentrum.blob;

public class P2PBlobBlockLayout {
  final long blobLengthInBytes;
  final int blockLength;
  
  public P2PBlobBlockLayout(long blobLengthInBytes, int blockLength) {
    this.blobLengthInBytes=blobLengthInBytes;
    this.blockLength=blockLength;
  }
  
  public int getLengthOfUnEvenBlock() {
    return (int) (blobLengthInBytes%blockLength);
  }

  public int getLengthOfEvenBlock() {
    return blockLength;
  }

  public long getLengthOfBlob() {
    return blobLengthInBytes;
  }

  public int getNumberOfBlocks() {
    int nbBlocks=(int) (blobLengthInBytes/blockLength);
    if(getLengthOfUnEvenBlock()!=0){
      nbBlocks++;
    }
    return nbBlocks;
  }

  public int getNumberOfEvenBlocks() {
    int nbBlocks=(int) (blobLengthInBytes/blockLength);
    return nbBlocks;
  }

  public int getIndexOfUnEvenBlock(){
    if(getLengthOfUnEvenBlock()==0){
      return -1; //All blocks are even
    }
    int numberOfEvenBlocks=getNumberOfEvenBlocks();
    return numberOfEvenBlocks;
  }
  
  public int getLengthOfBlock(int blockIndex) {
    if(blockIndex==getIndexOfUnEvenBlock()){
      return getLengthOfUnEvenBlock();
    }
    else if(blockIndex<0 || blockIndex>getNumberOfEvenBlocks()){
      throw new IndexOutOfBoundsException();
    }

    return getLengthOfEvenBlock();
  }

  public long getOffsetOfBlock(int blockIndex) {
    return blockIndex*getLengthOfEvenBlock();
  }

}
