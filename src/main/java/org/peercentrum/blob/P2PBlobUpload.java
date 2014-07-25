package org.peercentrum.blob;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;

public class P2PBlobUpload implements AutoCloseable {
  P2PBlobHashList hashList;
  FileLock fileLock;
  RandomAccessFile raf; //Only one or the other is set
  byte[] fullBlobContent;
  
  public P2PBlobUpload(File fileToUpload) throws Exception {
    raf = new RandomAccessFile(fileToUpload, "rw");
    fileLock=raf.getChannel().lock();
    hashList=P2PBlobHashList.createFromFileChannel(getBlockLength(), raf.getChannel());
  }

  public P2PBlobUpload(byte[] blobContent) {
    hashList=P2PBlobHashList.createFromBytes(getBlockLength(), blobContent);
    fullBlobContent=blobContent;
  }

  public P2PBlobHashList getHashList() {
    return hashList;
  }

  public int getBlockLength() {
    return P2PBlobApplication.BLOCK_SIZE;
  }
  
  public ByteBuffer getBlock(int blockIndex) throws Exception{
    if(blockIndex>=hashList.size()){
      throw new Exception("blockIndex "+blockIndex+" out of range "+hashList.size());
    }
    int offset=blockIndex*getBlockLength();
    boolean isLastBlock=blockIndex+1==hashList.size();

    if(fullBlobContent!=null){
      int blockLength;
      if(isLastBlock){
        blockLength=fullBlobContent.length-offset;
      }
      else{
        blockLength=getBlockLength();
      }
      return ByteBuffer.wrap(fullBlobContent, offset, blockLength);
    }
    else if(raf!=null){
      int blockLength;
      if(isLastBlock){
        blockLength=(int) (raf.length()-offset);
      }
      else{
        blockLength=getBlockLength();
      }

      raf.getChannel().position(offset);
      ByteBuffer bbToReturn=ByteBuffer.allocateDirect(blockLength);
      raf.getChannel().read(bbToReturn);
      bbToReturn.flip();
      return bbToReturn;
    }
    throw new Exception("Should have either a file or blobbytes at this point");
  }

  @Override
  public void close() throws Exception {
    if(fileLock!=null){
      fileLock.close();
      raf.close();
    }
  }

  public long getBlobLength() throws IOException {
    if(raf!=null){
      return raf.length();
    }
    else{
      return this.fullBlobContent.length;
    }
  }

}
