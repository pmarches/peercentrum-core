package org.peercentrum.blob;

import java.nio.ByteBuffer;

import org.bitcoinj.core.Coin;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.P2PBlobResponseMsg;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkClientTCPConnection;
import org.peercentrum.settlement.SettlementApplicationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import io.netty.util.concurrent.DefaultProgressivePromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

public class P2PBlobStandaloneClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobStandaloneClient.class);
  PriceLimits ourPriceModel=null;
	
	NetworkClientTCPConnection connection;
	SettlementApplicationClient settlementClient;
	
	public P2PBlobStandaloneClient(NetworkClientTCPConnection connection, TopLevelConfig config, SettlementApplicationClient settlementClient) {
	  this.connection=connection;
	  this.settlementClient=settlementClient;
	  P2PBlobConfig blobConfig=(P2PBlobConfig) config.getAppConfig(P2PBlobConfig.class);
	  ourPriceModel=blobConfig.getPriceLimits();
	}

	public Future<P2PBlobStoredBlob> downloadAll(final P2PBlobStoredBlob storedBlobDestination) {
		//FIXME Is this the proper thread?
		final DefaultProgressivePromise<P2PBlobStoredBlob> downloadPromise=new DefaultProgressivePromise<>(GlobalEventExecutor.INSTANCE);
		if(storedBlobDestination.isBlobDownloadComplete()){
			return downloadPromise.setSuccess(storedBlobDestination);
		}

		PB.P2PBlobRequestMsg.Builder appLevelMessage=PB.P2PBlobRequestMsg.newBuilder();
		PB.P2PBlobBlobRequestMsg.Builder oneBlobRequestMsg=PB.P2PBlobBlobRequestMsg.newBuilder();
		
		oneBlobRequestMsg.setRequestedHash(ByteString.copyFrom(storedBlobDestination.blobHash.getBytes()));
		boolean requestMetaData=storedBlobDestination.hasMetaData()==false;
		oneBlobRequestMsg.setRequestMetaData(requestMetaData);

		if(storedBlobDestination.getHashList()!=null){
			P2PBlobRangeSet missingRanges=storedBlobDestination.getMissingRanges();
			oneBlobRequestMsg.addAllRequestedRanges(missingRanges.toP2PBlobRangeMsgList());
		}
		appLevelMessage.addBlobRequest(oneBlobRequestMsg);

		Future<PB.P2PBlobResponseMsg> responseFuture = connection.sendRequestMsg(
				P2PBlobApplication.APP_ID, appLevelMessage.build(), PB.P2PBlobResponseMsg.class);

		responseFuture.addListener(new GenericFutureListener<Future<? super P2PBlobResponseMsg>>() {
			@Override
			public void operationComplete(Future<? super P2PBlobResponseMsg> future) throws Exception {
				P2PBlobResponseMsg response=(P2PBlobResponseMsg) future.get();
				if(storedBlobDestination.hasMetaData()==false){
					if(response.hasMetaData()==false){
						downloadPromise.setFailure(new Exception("The response did not include the metaData even tough we requested it."));
						future.cancel(true);
						LOGGER.error("The response did not include the metaData even tough we requested one.");
						return;
					}
					PB.P2PBlobMetaDataMsg metaData=response.getMetaData();
					storedBlobDestination.setMetaData(metaData);
				}

				for(PB.P2PBlobBlockMsg blobBlockMsg : response.getBlobBytesList()){
				  storedBlobDestination.maybeAcceptBlobBytes(blobBlockMsg);
				}
				
        if(response.hasDataTransferQuote()){
          PB.P2PBlobDataTransferQuoteMsg quoteMsg=response.getDataTransferQuote();
          if(quoteMsg.hasSatoshiPerOutgoingGigaByte()==false){
            LOGGER.error("Incomplete quote message {}", quoteMsg);
          }
          else if(response.getLocalBlockInventoryCount()==0){
            LOGGER.error("Missing block inventory from response {}", response);
            //The networkServer is asking for money even tough he does not have blocks?
          }
          else{
            P2PBlobRangeSet serverSideBlockInventory = new P2PBlobRangeSet(response.getLocalBlockInventoryList());
            P2PBlobRangeSet blocksWeAreMissing=serverSideBlockInventory.minus(storedBlobDestination.getLocalBlockRange());
            LOGGER.debug("blocksWeAreMissing={}", blocksWeAreMissing);
            
            //Determine how much data we are asking
            long nbBytesWeHaveToPayFor=storedBlobDestination.getNumberOfBytesInBlockRange(blocksWeAreMissing);
            LOGGER.debug("nbBytesWeHaveToPayFor={}", nbBytesWeHaveToPayFor);
            
            //TODO Determine if the price is fair (check the market? maybe we can download elsewhere?)
            //Determine if we are willing to pay such an amount [per byte] (check the config file)
            long serverPricePerGB=quoteMsg.getSatoshiPerOutgoingGigaByte();
            if(ourPriceModel.isDownloadPriceWithinLimits(serverPricePerGB)){
              //Determine if we have that much money...
              Coin microPaymentAmount=Coin.valueOf((nbBytesWeHaveToPayFor*serverPricePerGB)/1_000_000);
              
              settlementClient.makeMicroPayment(P2PBlobApplication.APP_ID, microPaymentAmount);
            }
          }
        }

				downloadPromise.setSuccess(storedBlobDestination);
			}
		});

		return downloadPromise;
	}
	
  public void upload(P2PBlobStoredBlob upload) throws Exception {
    PB.P2PBlobRequestMsg.Builder topLevelReq=PB.P2PBlobRequestMsg.newBuilder();
    PB.P2PBlobUploadRequestMsg.Builder uploadRequest=PB.P2PBlobUploadRequestMsg.newBuilder();

    P2PBlobBlockLayout blockLayout=upload.getBlockLayout();
    ByteBuffer blockBytes=ByteBuffer.allocateDirect(blockLayout.getLengthOfEvenBlock());
    for(int i=0; i<blockLayout.getNumberOfBlocks(); i++){
      upload.getBlock(i, blockBytes);
      PB.P2PBlobBlockMsg.Builder oneBlock=PB.P2PBlobBlockMsg.newBuilder();
      oneBlock.setBlockIndex(i);
      oneBlock.setBlobBytes(ByteString.copyFrom(blockBytes));
      uploadRequest.addBlocks(oneBlock);
    }
    PB.P2PBlobMetaDataMsg.Builder metaData=PB.P2PBlobMetaDataMsg.newBuilder();
    metaData.setBlobLength(blockLayout.getLengthOfBlob());
    metaData.setBlockSize(blockLayout.getLengthOfEvenBlock());
    metaData.setHashList(upload.getHashList().toHashListMsg());
    uploadRequest.setMetaData(metaData);
    topLevelReq.addUploadRequest(uploadRequest);
    Future<PB.P2PBlobRequestMsg> responseFuture=connection.sendRequestMsg(P2PBlobApplication.APP_ID, topLevelReq.build());
    responseFuture.sync();
  }

}
