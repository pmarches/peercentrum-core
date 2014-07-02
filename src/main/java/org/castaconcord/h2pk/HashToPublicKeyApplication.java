package org.castaconcord.h2pk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import org.castaconcord.consensusprocess.ConsensusProcess;
import org.castaconcord.consensusprocess.ConsensusThreshold;
import org.castaconcord.consensusprocess.ProposedTransactions;
import org.castaconcord.consensusprocess.UniqueNodeList;
import org.castaconcord.core.BazarroApplicationIdentifier;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncQuery;
import org.castaconcord.core.ProtocolBuffer.H2PKProposedTransactions;
import org.castaconcord.core.ProtocolBuffer.HashToPublicKeyMessage;
import org.castaconcord.network.BaseBazarroApplicationMessageHandler;
import org.castaconcord.network.BazarroNetworkClient;
import org.castaconcord.network.BazarroNetworkServer;
import org.castaconcord.network.HeaderAndPayload;
import org.castaconcord.network.ProtobufByteBufCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;

public class HashToPublicKeyApplication extends BaseBazarroApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashToPublicKeyApplication.class);
	public static final BazarroApplicationIdentifier APP_ID=new BazarroApplicationIdentifier(HashToPublicKeyApplication.class.getSimpleName().getBytes());
	ConsensusProcess consensus;
	HashToPublicKeyTXEvaluator txEvaluator;
	HashToPublicKeyDB db;
	BazarroNetworkClient networkClient;
		
	public HashToPublicKeyApplication(BazarroNetworkServer nodeServer, HashToPublicKeyDB db, UniqueNodeList unl) {
		super(nodeServer);
		if(unl==null){
			throw new NullPointerException("NULL UNL not allowed");
		}

		networkClient=new BazarroNetworkClient(nodeServer.getLocalNodeId(), nodeServer.getNodeDatabase());
		this.db=db;
		txEvaluator=new HashToPublicKeyTXEvaluator(db);
		final int DB_CLOSECYCLE_PERIOD_MS = 3000;
		ConsensusThreshold consensusThreshold=new ConsensusThreshold(50, 100, 10, DB_CLOSECYCLE_PERIOD_MS/5);
		consensus=new ConsensusProcess(nodeServer.getLocalNodeId(), txEvaluator, unl, db, consensusThreshold) {
			@Override
			protected void broadcastProposedTransactions(ProposedTransactions ourProposedTx) {
				try {
					//TODO use generics instead of cast
					ProposedTransactions<HashToPublicKeyTransaction> proposedTX=ourProposedTx;
					LOGGER.debug("broadcasting our {} proposed transactions", proposedTX.size());
					ProtocolBuffer.HashToPublicKeyMessage proposedTXMsg = encodeProposedTXToMsg(proposedTX);
					for(BazarroNodeIdentifier aValidator:unl){
						LOGGER.debug("Sending proposedTX to {}", aValidator);
						networkClient.sendRequest(aValidator, getApplicationId(), proposedTXMsg);
					}
				} catch (Exception e) {
					LOGGER.error("Failed to broadcast proposed TX "+ourProposedTx, e);
				}
			}
		};
	}
	
	public void startDBCloseProcess(int nbStepsToExecute){
		bringLocalDBUptoDate();
		Thread dbCloseProcess;
		if(nbStepsToExecute==0){
			dbCloseProcess=new Thread(consensus.continuousDatabaseCloseProcess);
		}
		else{
			if(nbStepsToExecute!=1){
				throw new RuntimeException("Not implemented");
			}
			dbCloseProcess=new Thread(consensus.stepDatabaseCloseProcess);
		}
		dbCloseProcess.setName(super.clientOrServer.getLocalNodeId().toString());
		dbCloseProcess.start();
	}
	
	protected void bringLocalDBUptoDate() {
		try {
			//TODO We should ask each validator for the hash of the latest DB and compare
			//TODO Then, we can download the DB with the validated hash

			ProtocolBuffer.HashToPublicKeyMessage.Builder appLevelMsg = ProtocolBuffer.HashToPublicKeyMessage.newBuilder();
			ProtocolBuffer.H2PKDBSyncQuery.Builder queryMsg=ProtocolBuffer.H2PKDBSyncQuery.newBuilder();
			boolean requestFullSync=true;
			if(requestFullSync){
				queryMsg.setBeginDbVersionNumber(0);
			}
			appLevelMsg.setDbSyncQuery(queryMsg.build());
			BazarroNodeIdentifier remoteId=consensus.unl.getOneExcluding(this.clientOrServer.getLocalNodeId());
			HashToPublicKeyMessage appLevelResponse = networkClient.sendRequest(remoteId, getApplicationId(), appLevelMsg.build()).get();
			if(appLevelResponse.hasDbSyncResponse()){
				ProtocolBuffer.H2PKDBSyncResponse dbSyncResponse = appLevelResponse.getDbSyncResponse();
				db.integrateSyncUnit(dbSyncResponse);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
		try {
			LOGGER.debug("generateReponseFromQuery");
			ProtocolBuffer.HashToPublicKeyMessage.Builder appLevelResponseBuilder = ProtocolBuffer.HashToPublicKeyMessage.newBuilder();

			ProtocolBuffer.HashToPublicKeyMessage appRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, ProtocolBuffer.HashToPublicKeyMessage.class);
			if(appRequest.hasLocalTransaction()){
				LOGGER.debug("received local transaction");
				ProtocolBuffer.HashToPublicKeyTransaction localTXMsg = appRequest.getLocalTransaction();
				// TODO Convert payload to one of these TX object
				HashToPublicKeyTransaction localTransaction=new HashToPublicKeyTransaction(localTXMsg);
				consensus.receiveLocalTransaction(localTransaction);
			}
			
			if(appRequest.hasProposedTx()){
				LOGGER.trace("request has proposed TX");
				H2PKProposedTransactions proposedTxMsg = appRequest.getProposedTx();
				ProposedTransactions<HashToPublicKeyTransaction> proposedTransaction = decodeMsgToProposedTX(proposedTxMsg);
				consensus.receiveProposedTransactions(proposedTransaction);
			}

			if(appRequest.hasDbSyncQuery()){
				LOGGER.trace("request hasDbSyncQuery");
				H2PKDBSyncQuery dbSyncQuery = appRequest.getDbSyncQuery();
				db.generateDbSyncResponse(dbSyncQuery, appLevelResponseBuilder);
			}
			
			ByteBuf payload=ProtobufByteBufCodec.encodeNoLengthPrefix(appLevelResponseBuilder);
			return new HeaderAndPayload(newResponseHeaderForRequest(receivedMessage), payload);
		} catch (Exception e) {
			LOGGER.error("failed to generate response from query", e);
			return null;
		}
	}

	protected ProposedTransactions<HashToPublicKeyTransaction> decodeMsgToProposedTX(ProtocolBuffer.H2PKProposedTransactions proposedTxMsg) {
		UnsignedLong proposalDbVersion=UnsignedLong.valueOf(proposedTxMsg.getDbVersionNumber());
		BazarroNodeIdentifier senderPK=new BazarroNodeIdentifier(proposedTxMsg.getProposedBy().toByteArray());
		ProposedTransactions<HashToPublicKeyTransaction> proposedTransaction=new ProposedTransactions<HashToPublicKeyTransaction>(proposalDbVersion, senderPK);

		List<ProtocolBuffer.HashToPublicKeyTransaction> txMsgList = proposedTxMsg.getProposedTransactionsList();
		for(ProtocolBuffer.HashToPublicKeyTransaction txMsg:txMsgList){
			proposedTransaction.addTransactions(new HashToPublicKeyTransaction(txMsg));
		}
		return proposedTransaction;
	}

	protected HashToPublicKeyMessage encodeProposedTXToMsg(ProposedTransactions<HashToPublicKeyTransaction> proposedTX) {
		ProtocolBuffer.H2PKProposedTransactions.Builder proposedTxMsg=ProtocolBuffer.H2PKProposedTransactions.newBuilder();
		proposedTxMsg.setDbVersionNumber(proposedTX.getDbVersionNumber());
		proposedTxMsg.setProposedBy(ByteString.copyFrom(proposedTX.getFrom().getBytes()));
		for(HashToPublicKeyTransaction tx : proposedTX){
			proposedTxMsg.addProposedTransactions(tx.toMessage());
		}
		
		ProtocolBuffer.HashToPublicKeyMessage.Builder applicationLevelBuilder = ProtocolBuffer.HashToPublicKeyMessage.newBuilder(); 
		applicationLevelBuilder.setProposedTx(proposedTxMsg);
		return applicationLevelBuilder.build();
//		byte[] applicationLevelBytes=applicationLevelBuilder.build().toByteArray();
//		return Unpooled.copiedBuffer(applicationLevelBytes);
	}

	@Override
	public BazarroApplicationIdentifier getApplicationId() {
		return APP_ID;
	}
}
