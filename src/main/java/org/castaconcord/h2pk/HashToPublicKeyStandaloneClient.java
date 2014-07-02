package org.castaconcord.h2pk;

import io.netty.util.concurrent.Future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.castaconcord.core.BazarroNodeDatabase;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.GenericResponse;
import org.castaconcord.core.ProtocolBuffer.HashToPublicKeyMessage;
import org.castaconcord.network.BazarroNetworkClient;

import com.google.protobuf.ByteString;

public class HashToPublicKeyStandaloneClient extends BazarroNetworkClient {
	private BazarroNodeIdentifier remoteHost;

	public HashToPublicKeyStandaloneClient(BazarroNodeIdentifier remoteHost, BazarroNodeIdentifier thisNodeIdentifier, BazarroNodeDatabase nodeDb) {
		super(thisNodeIdentifier, nodeDb);
		this.remoteHost=remoteHost;
	}
	
	public void registerForAddress(BazarroHashIdentifier address, BazarroPublicKeyIdentifier publicKey) throws Exception {
		//TODO Add expiration
		//TODO Add real signature here
		BazarroHashPointerSignature signature = new BazarroHashPointerSignature("FAKE sig data".getBytes());
		HashToPublicKeyTransaction txObject=new HashToPublicKeyTransaction(address, publicKey, true, signature);
		
		ProtocolBuffer.HashToPublicKeyMessage.Builder appLevelMessage=ProtocolBuffer.HashToPublicKeyMessage.newBuilder();
		appLevelMessage.setLocalTransaction(txObject.toMessage());
		Future<HashToPublicKeyMessage> registrationResponseFuture = sendRequest(remoteHost, HashToPublicKeyApplication.APP_ID, appLevelMessage.build());
		GenericResponse genericResponse = registrationResponseFuture.get().getGenericResponse();
		if(genericResponse!=null && genericResponse.getErrorCode()!=0){
			throw new Exception("registration of address "+address+" failed because "+genericResponse.getErrorMessage()+". error code "+genericResponse.getErrorCode());
		}
	}
	
	public List<BazarroNodeIdentifier> getMembershipForAddress(BazarroHashIdentifier address) throws Exception {
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

		ArrayList<BazarroNodeIdentifier> membership = new ArrayList<BazarroNodeIdentifier>(membershipResponse.size());
		for (ByteString registeredBytes : membershipResponse) {
			membership.add(new BazarroNodeIdentifier(registeredBytes.toByteArray()));
		}
		return membership;
	}

}
