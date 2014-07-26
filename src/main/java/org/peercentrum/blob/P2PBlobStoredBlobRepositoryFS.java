package org.peercentrum.blob;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.peercentrum.h2pk.HashIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class P2PBlobStoredBlobRepositoryFS extends P2PBlobStoredBlob {
  protected File blobFile;
  private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobStoredBlobRepositoryFS.class);
  
  public P2PBlobStoredBlobRepositoryFS(File blobFile, HashIdentifier blobHash, P2PBlobHashList hashList,
      P2PBlobRangeSet localBlockRange, long blobByteSize, int blockSize) {
    super(blobHash, hashList, localBlockRange, blobByteSize, blockSize);
    this.blobFile=blobFile;
  }

  public P2PBlobStoredBlobRepositoryFS(File blobFile) {
    super(null, null, null, blobFile.length(), P2PBlobApplication.BLOCK_SIZE);
    this.blobFile=blobFile;
  }

  @Override
  public ByteString getBytesRange(long bytesOffset, int length) throws Exception {
    ByteBuffer dataBlock = ByteBuffer.allocate(length);
    getBytesRange(bytesOffset, dataBlock);
    return ByteString.copyFrom(dataBlock); //FIXME avoid copying the bytes twice
  }

  @Override
  public void getBytesRange(long offset, ByteBuffer buffer) throws Exception {
    RandomAccessFile rafBlob=new RandomAccessFile(blobFile, "r");
    FileChannel fileChannel=rafBlob.getChannel();
    fileChannel.position(offset);

    fileChannel.read(buffer);
    buffer.flip();
    fileChannel.close();
    rafBlob.close();
  }

  /**
   * This function assumes the bytes have been validated against the hashList beforehand.
   */
  @Override
  protected void acceptValidatedBlobBytes(int blockIndex, ByteBuffer blobBlockBytes) throws Exception {
    if(isBlobDownloadComplete()){
      LOGGER.warn("Trying to insert bytes {} for blob {} that is already complete", blockIndex, getBlobIdentifier());
      return;
    }
    if(localBlockRange.ranges.contains(blockIndex)){
      LOGGER.warn("We already have the block {} for blob {}", blockIndex, getBlobIdentifier());
      return;
    }

    RandomAccessFile rafBlob=new RandomAccessFile(blobFile, "rw");
    FileChannel fileChannel=rafBlob.getChannel();
    long bytesOffset=blockLayout.getBlockOffset(blockIndex);
    fileChannel.position(bytesOffset);
    fileChannel.write(blobBlockBytes);
    fileChannel.close();
    rafBlob.close();
  }

}
