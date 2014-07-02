package org.castaconcord.blob;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.castaconcord.blob.P2PBlobRangeSet.DiscreteIterator;
import org.castaconcord.core.ApplicationIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.HeaderMessage;
import org.castaconcord.core.ProtocolBuffer.GenericResponse;
import org.castaconcord.core.ProtocolBuffer.P2PBlobRequest;
import org.castaconcord.core.ProtocolBuffer.P2PBlobResponse;
import org.castaconcord.h2pk.HashIdentifier;
import org.castaconcord.network.BaseApplicationMessageHandler;
import org.castaconcord.network.NetworkServer;
import org.castaconcord.network.HeaderAndPayload;
import org.castaconcord.network.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PBlobApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier(P2PBlobApplication.class.getSimpleName().getBytes());
	public static final int BLOCK_SIZE = 256*1024;

	protected P2PBlobRepository blobRepository;
	
	public P2PBlobApplication(NetworkServer clientOrServer, P2PBlobRepository blobRepository) {
		super(clientOrServer);
		this.blobRepository=blobRepository;
		clientOrServer.addApplicationHandler(this);
	}

	@Override
	public ApplicationIdentifier getApplicationId() {
		return APP_ID;
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedRequest) {
		try {
			ProtocolBuffer.P2PBlobRequest request=(P2PBlobRequest)ProtobufByteBufCodec.decodeNoLengthPrefix(receivedRequest.payload, P2PBlobRequest.class);
			ProtocolBuffer.P2PBlobResponse.Builder appLevelResponse=P2PBlobResponse.newBuilder();
			
			GenericResponse.Builder genericResponse=null;
			if(request.hasRequestedHash()==false){
				LOGGER.error("request {} is missing the blob hash", request);
				genericResponse=GenericResponse.newBuilder();
				genericResponse.setErrorCode(1);
				genericResponse.setErrorMessage("Missing requested blob hash");
				appLevelResponse.setGenericResponse(genericResponse);
			}
			if(genericResponse!=null){
				ByteBuf payload=ProtobufByteBufCodec.encodeNoLengthPrefix(appLevelResponse);
				return new HeaderAndPayload(newResponseHeaderForRequest(receivedRequest), payload);
			}

			//The request looks sane...
			completeResponse(request, appLevelResponse);
			ByteBuf appSpecificResponseBytes=ProtobufByteBufCodec.encodeNoLengthPrefix(appLevelResponse);
			HeaderMessage.Builder responseHeader = newResponseHeaderForRequest(receivedRequest);
			return new HeaderAndPayload(responseHeader, appSpecificResponseBytes);
		} catch (Exception e) {
			LOGGER.error("Failed to generateResponseFromQuery", e);
			return null;
		}
	}

	private void completeResponse(ProtocolBuffer.P2PBlobRequest request, ProtocolBuffer.P2PBlobResponse.Builder appLevelResponseBuilder) throws Exception {
		HashIdentifier blobHash=new HashIdentifier(request.getRequestedHash().toByteArray());
		LOGGER.debug("Got a request for blob hash {}", blobHash);
		P2PBlobStoredBlob storedBlob=blobRepository.getStoredBlob(blobHash);
		if(storedBlob.isBlobDownloadComplete()==false){
			throw new RuntimeException("Not implemented"); //We should be able to seed other nodes with the partial bytes we have...
		}

		if(request.hasRequestedHash() && request.getRequestHashList()){
			appLevelResponseBuilder.setHashList(storedBlob.getHashList().toHashListMsg());
		}
		if(request.hasRequestBlobLength()){
			appLevelResponseBuilder.setBlobLength(storedBlob.getBlobLength());
		}

		P2PBlobHashList hashList =storedBlob.getHashList();
		P2PBlobRangeSet requestedRanges;
		if(request.getRequestedRangesCount()!=0){
			requestedRanges=new P2PBlobRangeSet(request.getRequestedRangesList(), hashList.size()-1);
		}
		else{
			requestedRanges=new P2PBlobRangeSet(0, hashList.size()-1);
		}
		DiscreteIterator di = requestedRanges.discreteIterator();
		while(di.hasNext()){
			int blockIndex=di.next();
			long offset=blockIndex*BLOCK_SIZE;
			ProtocolBuffer.P2PBlobBlockMsg.Builder blockMsg=ProtocolBuffer.P2PBlobBlockMsg.newBuilder();
			blockMsg.setBlockIndex(blockIndex);
			blockMsg.setBlobBytes(storedBlob.getBytesRange(offset, BLOCK_SIZE));
			appLevelResponseBuilder.addBlobBytes(blockMsg);
		}

		/////////TODO enable all this..///////
//		if(request.hasMaximumBlobLength()){
//			long maximumBlobLength=request.getMaximumBlobLength();
//			throw new RuntimeException("Not implemented");
//		}
	}
}
