package org.peercentrum.blob;

import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ProtocolBuffer;
import org.peercentrum.core.ProtocolBuffer.P2PBlobResponse;
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

public class P2PBlobStandaloneClient extends NetworkClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobStandaloneClient.class);
	
	protected NodeIdentifier remoteHost;

	public P2PBlobStandaloneClient(NodeIdentifier remoteHost, NodeIdentifier thisNodeId, NodeDatabase nodeDatabase) {
		super(thisNodeId, nodeDatabase);
		this.remoteHost=remoteHost;
	}

	public Future<P2PBlobStoredBlob> downloadAll(final P2PBlobStoredBlob transitStatus) {
		//FIXME Is this the proper thread?
		final DefaultProgressivePromise<P2PBlobStoredBlob> downloadPromise=new DefaultProgressivePromise<>(GlobalEventExecutor.INSTANCE);
		if(transitStatus.isBlobDownloadComplete()){
			return downloadPromise.setSuccess(transitStatus);
		}

		ProtocolBuffer.P2PBlobRequest.Builder appLevelMessage=ProtocolBuffer.P2PBlobRequest.newBuilder();
		appLevelMessage.setRequestedHash(ByteString.copyFrom(transitStatus.blobHash.getBytes()));
		appLevelMessage.setRequestHashList(transitStatus.getHashList()==null); //Request the hashList if we do not have it
		appLevelMessage.setRequestBlobLength(true); //Does not cost much

		if(transitStatus.getHashList()!=null){
			P2PBlobRangeSet missingRanges=transitStatus.getMissingRanges();
			appLevelMessage.addAllRequestedRanges(missingRanges.toP2PBlobRangeMsgList());
		}

		NetworkClientConnection connection = maybeOpenConnectionToPeer(remoteHost);
		Future<ProtocolBuffer.P2PBlobResponse> responseFuture = connection.sendRequestMsg(
				P2PBlobApplication.APP_ID, appLevelMessage.build(), ProtocolBuffer.P2PBlobResponse.class);

		responseFuture.addListener(new GenericFutureListener<Future<? super P2PBlobResponse>>() {
			@Override
			public void operationComplete(Future<? super P2PBlobResponse> future) throws Exception {
				P2PBlobResponse response=(P2PBlobResponse) future.get();
				if(transitStatus.getHashList()==null){
					if(response.hasHashList()==false){
						downloadPromise.setFailure(new Exception("The response did not include the hashList even tough we requested one."));
						future.cancel(true);
						LOGGER.error("The response did not include the hashList even tough we requested one.");
						return;
					}
					if(response.hasBlobLength()){
						transitStatus.blobLengthInBytes=response.getBlobLength();
					}
					transitStatus.setHashList(new P2PBlobHashList(response.getHashList()));
				}

				for(ProtocolBuffer.P2PBlobBlockMsg blobBlockMsg : response.getBlobBytesList()){
					maybeAcceptBlobBytes(transitStatus, blobBlockMsg);
				}
				downloadPromise.setSuccess(transitStatus);
			}
		});

		return downloadPromise;
	}
	
	public void maybeAcceptBlobBytes(P2PBlobStoredBlob transitStatus, ProtocolBuffer.P2PBlobBlockMsg blobBlockMsg) {
		if(transitStatus.hashList==null){
			throw new RuntimeException("Need to receive the hashList before accepting blocks");
		}
		if(blobBlockMsg.hasBlockIndex()==false || blobBlockMsg.hasBlobBytes()==false){
			throw new RuntimeException("Missing blockIndex or blockBytes");
		}
		int currentBlockIndex=blobBlockMsg.getBlockIndex();
		byte[] blobBlockBytes=blobBlockMsg.getBlobBytes().toByteArray();
		
		HashIdentifier blockHash=transitStatus.hashList.get(currentBlockIndex);
		HashIdentifier blobBlockBytesHash=P2PBlobHashList.hashBytes(blobBlockBytes, 0 ,blobBlockBytes.length);
		if(blobBlockBytesHash.equals(blockHash)==false){
			LOGGER.error("The block "+currentBlockIndex+" does not hash to "+blockHash);
			return; //Throw exception?
		}
		transitStatus.acceptValidatedBlobBytes(currentBlockIndex, blobBlockBytes);
	}

}
