package org.peercentrum.blob;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import org.peercentrum.core.ProtocolBuffer;
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

	public P2PBlobHashList(ProtocolBuffer.P2PBlobHashListMsg hashListMsg) {
		for(ByteString hashMsg:hashListMsg.getHashBytesList()){
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

	public ProtocolBuffer.P2PBlobHashListMsg toHashListMsg() {
		ProtocolBuffer.P2PBlobHashListMsg.Builder hashListMsg=ProtocolBuffer.P2PBlobHashListMsg.newBuilder();
		for(HashIdentifier currentHash:this){
			hashListMsg.addHashBytes(currentHash.toByteString());
		}
		return hashListMsg.build();
	}

	//Not sure this should be in this class, ok for now..
	public static P2PBlobHashList createFromFile(File nonRepositoryFile) throws IOException {
		P2PBlobHashList hashList=new P2PBlobHashList();
		RandomAccessFile RAFToHash=new RandomAccessFile(nonRepositoryFile, "r");
		FileChannel fileChannel=RAFToHash.getChannel();

		ByteBuffer dataBlock = ByteBuffer.allocate(P2PBlobApplication.BLOCK_SIZE);
		while(true){
			int nbBytesRead = fileChannel.read(dataBlock);
			if(dataBlock.remaining()!=0 && nbBytesRead!=-1){
				continue;
			}
			if(dataBlock.limit()!=0){
				HashIdentifier blockHash=hashBytes(dataBlock.array(), dataBlock.arrayOffset(), dataBlock.position());
				hashList.add(blockHash);
				dataBlock.rewind();
			}
			if(nbBytesRead==-1){
				break;
			}
		}
		fileChannel.close();
		RAFToHash.close();
		return hashList;
	}

	public static HashIdentifier hashBytes(byte[] dataToHash, int offset, int length){
		SHA256Digest mda = new SHA256Digest();
		mda.update(dataToHash, offset, length);
		byte[] hashBytes = new byte[32];
		mda.doFinal(hashBytes, 0);
		return new HashIdentifier(hashBytes);
	}
	
	public static int getNumberOfBlocksForBlobSize(long blobByteSize) {
		double fractionalNumberOfBlocks=blobByteSize/(double) HASH_BYTE_SIZE;
		return (int) Math.ceil(fractionalNumberOfBlocks);
	}

}
