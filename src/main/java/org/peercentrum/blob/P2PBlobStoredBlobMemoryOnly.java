package org.peercentrum.blob;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.peercentrum.h2pk.HashIdentifier;

import com.google.protobuf.ByteString;

public class P2PBlobStoredBlobMemoryOnly extends P2PBlobStoredBlob {
	public ByteBuf validatedBlobContent=Unpooled.buffer();

	public P2PBlobStoredBlobMemoryOnly(HashIdentifier blobHash) {
		super(blobHash, null, null, -1, P2PBlobApplication.DEFAULT_BLOCK_SIZE);
	}
    
	public P2PBlobStoredBlobMemoryOnly(byte[] fullBlobContent) {
    super(null, P2PBlobHashList.createFromBytes(P2PBlobApplication.DEFAULT_BLOCK_SIZE, fullBlobContent),
        null, fullBlobContent.length, P2PBlobApplication.DEFAULT_BLOCK_SIZE);
    validatedBlobContent.writeBytes(fullBlobContent);
    blobHash=hashList.getTopLevelHash();
  }

  @Override
	protected void acceptValidatedBlobBytes(int blockIndex, ByteBuffer blobBlockBytes) {
		int blockStartsAt=(int) blockLayout.getOffsetOfBlock(blockIndex);
		validatedBlobContent.ensureWritable(blockStartsAt+blobBlockBytes.remaining());
		validatedBlobContent.writerIndex(blockStartsAt);
		validatedBlobContent.writeBytes(blobBlockBytes);
	}
	
	@Override
	public ByteString getBytesRange(long offset, int length) throws Exception {
		return ByteString.copyFrom(validatedBlobContent.array(), (int) offset, length);
	}

  @Override
  public void getBytesRange(long offset, ByteBuffer destinationBuffer) throws Exception {
    validatedBlobContent.getBytes((int) offset, destinationBuffer);
    destinationBuffer.flip();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((validatedBlobContent == null) ? 0 : validatedBlobContent.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    P2PBlobStoredBlobMemoryOnly other = (P2PBlobStoredBlobMemoryOnly) obj;
    if (validatedBlobContent == null) {
      if (other.validatedBlobContent != null)
        return false;
    } else if (!validatedBlobContent.equals(other.validatedBlobContent))
      return false;
    return true;
  }

  
}
