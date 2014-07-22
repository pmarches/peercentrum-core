package org.peercentrum.blob;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.peercentrum.h2pk.HashIdentifier;

import com.google.protobuf.ByteString;

public class P2PBlobStoredBlobRepositoryFS extends P2PBlobStoredBlob {
	protected File blobFile;
	protected P2PBlobRepositoryFS repository;

	public P2PBlobStoredBlobRepositoryFS(P2PBlobRepositoryFS repository, HashIdentifier blobHash, P2PBlobHashList hashList,
			P2PBlobRangeSet localBlockRange, long blobByteSize, int blockSize) {
		super(blobHash, hashList, localBlockRange, blobByteSize, blockSize);
		this.repository=repository;
	}

	@Override
	protected void acceptValidatedBlobBytes(int blockIndex, byte[] blobBlockBytes) {
		throw new RuntimeException("Not implemented");
	}
	
	@Override
	public ByteString getBytesRange(long bytesOffset, int length) throws Exception {
		File blobFile = getFileFromBlobId(super.blobHash);
		RandomAccessFile RAFToHash=new RandomAccessFile(blobFile, "r");
		FileChannel fileChannel=RAFToHash.getChannel();
		fileChannel.position(bytesOffset);
		
		ByteBuffer dataBlock = ByteBuffer.allocate(length);
		while(true){
			int nbBytesRead = fileChannel.read(dataBlock);
			if(nbBytesRead==-1 || dataBlock.remaining()==0){
				break;
			}
		}
		fileChannel.close();
		RAFToHash.close();
		dataBlock.flip();
		return ByteString.copyFrom(dataBlock); //FIXME avoid copying the bytes twice
	}


	protected File getFileFromBlobId(HashIdentifier blobId) {
		File blobFile=new File(repository.repositoryDirectory, blobId.toString()+".blob");
		return blobFile;
	}

	/**
	 * This function assumes the bytes have been validated against the hashList beforehand.
	 * @throws IOException 
	 *
	public void insertPartialBytes(Range<UnsignedLong> newDataRange, ByteBuffer blobBytes) throws IOException{
		if(isComplete()){
			LOGGER.warn("Trying to insert bytes {} for blob {} that is already complete", newDataRange, getBlobIdentifier());
			return;
		}
//		RangeSet<UnsignedLong> missingBytes = transitStatus.getMissingBytesRange(newDataRange);
		if(transitStatus.isRangeAlreadyDownloaded(newDataRange)){
			LOGGER.warn("We already have the bytes {} for blob {}", newDataRange, getBlobIdentifier());
			return;
		}

		//TODO somehow lock this file region?
		SeekableByteChannel regionChannel = Files.newByteChannel(pathOfBlobFile, StandardOpenOption.WRITE);
		regionChannel.write(blobBytes);
		regionChannel.close();
//		for(Range<UnsignedLong> rangeToWrite : missingBytes){
//			write(rangeToWrite, blobBytes); //Need to map the missingBytes to blobBytes! Might be slower than just overwriting whatever we have..
//		}
	}
*/

}
