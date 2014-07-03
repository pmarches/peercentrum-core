package org.peercentrum.h2pk;


import org.peercentrum.consensusprocess.ConsensusTransaction;
import org.peercentrum.core.ProtocolBuffer;

import com.google.protobuf.ByteString;

public class HashToPublicKeyTransaction extends ConsensusTransaction implements Cloneable {
	HashIdentifier address;
	PublicKeyIdentifier publicKey;
	HashPointerSignature signature;
	boolean isAppend;
	
	public HashToPublicKeyTransaction(ProtocolBuffer.HashToPublicKeyTransaction TXMsg) {
		address=new HashIdentifier(TXMsg.getAddress().toByteArray());
		publicKey=new PublicKeyIdentifier(TXMsg.getPublicKey().toByteArray());
		signature=new HashPointerSignature(TXMsg.getSignature().toByteArray());
		isAppend=TXMsg.getOperation()==ProtocolBuffer.HashToPublicKeyTransaction.OPERATION.APPEND;
	}
	
	public HashToPublicKeyTransaction(HashIdentifier address, PublicKeyIdentifier publicKey, 
			boolean isAppend, HashPointerSignature signature) {
		this.address=address;
		this.publicKey=publicKey;
		this.isAppend=isAppend;
		this.signature=signature;
	}

	@Override
	public HashToPublicKeyTransaction clone() throws CloneNotSupportedException {
		return new HashToPublicKeyTransaction(new HashIdentifier(address.getBytes()),
				new PublicKeyIdentifier(publicKey.getBytes()), isAppend, new HashPointerSignature(signature.getBytes()));
	}

	public ProtocolBuffer.HashToPublicKeyTransaction toMessage(){
		ProtocolBuffer.HashToPublicKeyTransaction.Builder TXMsg=ProtocolBuffer.HashToPublicKeyTransaction.newBuilder();
		TXMsg.setAddress(ByteString.copyFrom(address.getBytes()));
		TXMsg.setPublicKey(ByteString.copyFrom(publicKey.getBytes()));
		TXMsg.setSignature(ByteString.copyFrom(signature.getBytes()));
		if(isAppend){
			TXMsg.setOperation(ProtocolBuffer.HashToPublicKeyTransaction.OPERATION.APPEND);
		}
		else{
			TXMsg.setOperation(ProtocolBuffer.HashToPublicKeyTransaction.OPERATION.REMOVE);
		}
		return TXMsg.build();
	}

	public boolean isAppend() {
		return isAppend;
	}

	public boolean isRemove() {
		return !isAppend;
	}

	public HashIdentifier getAddress() {
		return address;
	}

	public PublicKeyIdentifier getPublicKey() {
		return publicKey;
	}

	@Override
	public String toString() {
		return "HashToPublicKeyTransaction [address=" + address + ", publicKey=" + publicKey + ", signature="
				+ signature + ", isAppend=" + isAppend + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + (isAppend ? 1231 : 1237);
		result = prime * result + ((publicKey == null) ? 0 : publicKey.hashCode());
		result = prime * result + ((signature == null) ? 0 : signature.hashCode());
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
		HashToPublicKeyTransaction other = (HashToPublicKeyTransaction) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (isAppend != other.isAppend)
			return false;
		if (publicKey == null) {
			if (other.publicKey != null)
				return false;
		} else if (!publicKey.equals(other.publicKey))
			return false;
		if (signature == null) {
			if (other.signature != null)
				return false;
		} else if (!signature.equals(other.signature))
			return false;
		return true;
	}

}
