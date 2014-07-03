package org.peercentrum.h2pk;

import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.GenericResponse;
import org.peercentrum.core.ProtocolBuffer.HashToPublicKeyMessage;
import org.peercentrum.network.NetworkClient;

import com.google.protobuf.ByteString;

public class HashToPublicKeyStandaloneClient extends NetworkClient {
	private NodeIdentifier remoteHost;

	public HashToPublicKeyStandaloneClient(NodeIdentifier remoteHost, NodeIdentifier thisNodeIdentifier, NodeDatabase nodeDb) {
		super(thisNodeIdentifier, nodeDb);
		this.remoteHost=remoteHost;
	}
	
	public void registerForAddress(HashIdentifier address, PublicKeyIdentifier publicKey) throws Exception {
		//TODO Add expiration
		//TODO Add real signature here
		HashPointerSignature signature = new HashPointerSignature("FAKE sig data".getBytes());
		HashToPublicKeyTransaction txObject=new HashToPublicKeyTransaction(address, publicKey, true, signature);
		
		ProtocolBuffer.HashToPublicKeyMessage.Builder appLevelMessage=ProtocolBuffer.HashToPublicKeyMessage.newBuilder();
		appLevelMessage.setLocalTransaction(txObject.toMessage());
		Future<HashToPublicKeyMessage> registrationResponseFuture = sendRequest(remoteHost, HashToPublicKeyApplication.APP_ID, appLevelMessage.build());
		GenericResponse genericResponse = registrationResponseFuture.get().getGenericResponse();
		if(genericResponse!=null && genericResponse.getErrorCode()!=0){
			throw new Exception("registration of address "+address+" failed because "+genericResponse.getErrorMessage()+". error code "+genericResponse.getErrorCode());
		}
	}
	
	public List<NodeIdentifier> getMembershipForAddress(HashIdentifier address) throws Exception {
		ProtocolBuffer.HashToPublicKeyMessage.Builder topLevelMessage=ProtocolBuffer.HashToPublicKeyMessage.newBuilder();
		topLevelMessage.setMembershipQuery(ByteString.copyFrom(address.getBytes()));

		Future<HashToPublicKeyMessage> registrationResponseFuture = sendRequest(remoteHost, HashToPublicKeyApplication.APP_ID, topLevelMessage.build());
		GenericResponse genericResponse = registrationResponseFuture.get().getGenericResponse();
		if(genericResponse!=null && genericResponse.getErrorCode()!=0){
			throw new Exception("failed to get membership of address "+address+" because "+genericResponse.getErrorMessage()+". error code "+genericResponse.getErrorCode());
		}
		List<ByteString> membershipResponse = registrationResponseFuture.get().getMembershipResponseList();
		if(membershipResponse==null || membershipResponse.isEmpty()){
			return Collections.emptyList();
		}

		ArrayList<NodeIdentifier> membership = new ArrayList<NodeIdentifier>(membershipResponse.size());
		for (ByteString registeredBytes : membershipResponse) {
			membership.add(new NodeIdentifier(registeredBytes.toByteArray()));
		}
		return membership;
	}

}
