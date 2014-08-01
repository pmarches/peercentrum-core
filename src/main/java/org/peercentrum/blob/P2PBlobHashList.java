package org.peercentrum.blob;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import org.peercentrum.core.PB;
import org.peercentrum.h2pk.HashIdentifier;
import org.spongycastle.crypto.digests.SHA256Digest;

import com.google.protobuf.ByteString;

public class P2PBlobHashList extends ArrayList<HashIdentifier> {
	public static final int HASH_BYTE_SIZE = 32; //Number of bytes per hash
	HashIdentifier topLevelHash;

	public P2PBlobHashList(byte[] concatenatedBlockHashes) {
		ByteBuf concatenatedHashes=Unpooled.wrappedBuffer(concatenatedBlockHashes);
		while(concatenatedHashes.readableBytes()!=0){
			byte[] blockHash=new byte[HASH_BYTE_SIZE];
			concatenatedHashes.readBytes(blockHash);
			add(new HashIdentifier(blockHash));
		}
	}

	public P2PBlobHashList(PB.P2PBlobHashListMsg hashListMsg) {
		for(ByteString hashMsg:hashListMsg.getHashOfEachBlockList()){
			add(new HashIdentifier(hashMsg.toByteArray()));
		}
	}

	public P2PBlobHashList() {
	}

	public HashIdentifier getTopLevelHash() {
		if(topLevelHash==null){
			SHA256Digest mda = new SHA256Digest();
			for(HashIdentifier hashOfBlock:this){
				mda.update(hashOfBlock.getBytes(), 0, hashOfBlock.getBytes().length);
			}
			byte[] hashBytes = new byte[32];
			mda.doFinal(hashBytes, 0);
			topLevelHash=new HashIdentifier(hashBytes);
		}
		return topLevelHash;
	}

	public PB.P2PBlobHashListMsg toHashListMsg() {
		PB.P2PBlobHashListMsg.Builder hashListMsg=PB.P2PBlobHashListMsg.newBuilder();
		for(HashIdentifier currentHash:this){
			hashListMsg.addHashOfEachBlock(currentHash.toByteString());
		}
		return hashListMsg.build();
	}

	//Not sure this should be in this class, ok for now..
	public static P2PBlobHashList createFromFileChannel(int blockLength, FileChannel fileChannelToRead) throws IOException {
		P2PBlobHashList hashList=new P2PBlobHashList();

		ByteBuffer dataBlock = ByteBuffer.allocate(blockLength);
		while(true){
			int nbBytesRead = fileChannelToRead.read(dataBlock);
      if(nbBytesRead==-1 && hashList.isEmpty()==false){
        break;
      }
      dataBlock.flip();
			if(dataBlock.limit()!=0){
				HashIdentifier blockHash=hashBytes(dataBlock);
				hashList.add(blockHash);
				dataBlock.rewind();
			}
		}
		return hashList;
	}

	public static HashIdentifier hashBytes(ByteBuffer blobBlockBytes){
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(blobBlockBytes);
      byte[] hashBytes = digest.digest();
      HashIdentifier hid = new HashIdentifier(hashBytes);
      return hid;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
	}
	
  public static P2PBlobHashList createFromFile(File nonRepositoryFile) throws IOException {
    RandomAccessFile rafToRead=new RandomAccessFile(nonRepositoryFile, "r");
    P2PBlobHashList hList=createFromFileChannel(P2PBlobApplication.DEFAULT_BLOCK_SIZE, rafToRead.getChannel());
    rafToRead.close();
    return hList;
  }

  public static P2PBlobHashList createFromBytes(int blockSize, byte[] blobContent) {
    P2PBlobHashList hashList=new P2PBlobHashList();
    ByteBuffer bbBlobContent=ByteBuffer.wrap(blobContent);
    P2PBlobBlockLayout blockLayout=new P2PBlobBlockLayout(blobContent.length, blockSize);
    while(true){
      int nbBytesInBlock=blockLayout.getLengthOfBlock(hashList.size());
      bbBlobContent.limit(bbBlobContent.position()+nbBytesInBlock);
      HashIdentifier blockHash=hashBytes(bbBlobContent);
      hashList.add(blockHash);
      if(bbBlobContent.limit()==bbBlobContent.capacity()){
        break;
      }
      bbBlobContent.position(bbBlobContent.limit());
    }
    return hashList;
  }

}
