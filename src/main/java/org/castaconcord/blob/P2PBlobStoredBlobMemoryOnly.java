package org.castaconcord.blob;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.castaconcord.h2pk.BazarroHashIdentifier;

import com.google.protobuf.ByteString;

public class P2PBlobStoredBlobMemoryOnly extends P2PBlobStoredBlob {
	public ByteBuf downloadedAndValidatedBlobContent=Unpooled.buffer();

	public P2PBlobStoredBlobMemoryOnly(BazarroHashIdentifier blobHash) {
		super(blobHash, null, null, -1);
	}
    
	@Override
	protected void acceptValidatedBlobBytes(int blockIndex, byte[] blobBlockBytes) {
		int blockStartsAt=blockIndex*P2PBlobApplication.BLOCK_SIZE;
		downloadedAndValidatedBlobContent.ensureWritable(blockStartsAt+blobBlockBytes.length);
		downloadedAndValidatedBlobContent.setBytes(blockStartsAt, blobBlockBytes);
		if(downloadedAndValidatedBlobContent.writerIndex()<blockStartsAt+blobBlockBytes.length){
			downloadedAndValidatedBlobContent.writerIndex(blockStartsAt+blobBlockBytes.length);
		}
	}
	
	@Override
	public ByteString getBytesRange(long offset, int length) throws Exception {
		return ByteString.copyFrom(downloadedAndValidatedBlobContent.array(), (int) offset, length);
	}

}
