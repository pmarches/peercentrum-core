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
  protected P2PBlobRepositoryFS repository;
  private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobStoredBlobRepositoryFS.class);
  
  public P2PBlobStoredBlobRepositoryFS(P2PBlobRepositoryFS repository, HashIdentifier blobHash, P2PBlobHashList hashList,
      P2PBlobRangeSet localBlockRange, long blobByteSize, int blockSize) {
    super(blobHash, hashList, localBlockRange, blobByteSize, blockSize);
    this.repository=repository;
  }

  @Override
  public ByteString getBytesRange(long bytesOffset, int length) throws Exception {
    File blobFile = getFileFromBlobId();
    RandomAccessFile rafBlob=new RandomAccessFile(blobFile, "r");
    FileChannel fileChannel=rafBlob.getChannel();
    fileChannel.position(bytesOffset);

    ByteBuffer dataBlock = ByteBuffer.allocate(length);
    fileChannel.read(dataBlock);
    dataBlock.flip();
    fileChannel.close();
    rafBlob.close();
    return ByteString.copyFrom(dataBlock); //FIXME avoid copying the bytes twice
  }


  protected File getFileFromBlobId() {
    File blobFile=new File(repository.repositoryDirectory, super.blobHash.toString()+".blob");
    return blobFile;
  }

  /**
   * This function assumes the bytes have been validated against the hashList beforehand.
   */
  @Override
  protected void acceptValidatedBlobBytes(int blockIndex, byte[] blobBlockBytes) throws Exception {
    if(isBlobDownloadComplete()){
      LOGGER.warn("Trying to insert bytes {} for blob {} that is already complete", blockIndex, getBlobIdentifier());
      return;
    }
    //		RangeSet<UnsignedLong> missingBytes = transitStatus.getMissingBytesRange(newDataRange);
    if(localBlockRange.ranges.contains(blockIndex)){
      LOGGER.warn("We already have the block {} for blob {}", blockIndex, getBlobIdentifier());
      return;
    }

    File blobFile = getFileFromBlobId();
    RandomAccessFile rafBlob=new RandomAccessFile(blobFile, "r");
    FileChannel fileChannel=rafBlob.getChannel();
    long bytesOffset;
    fileChannel.position(bytesOffset);

    fileChannel.close();
    rafBlob.close();
  }

}
