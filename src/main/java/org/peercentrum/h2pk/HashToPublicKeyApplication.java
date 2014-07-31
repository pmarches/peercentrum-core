package org.peercentrum.h2pk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import org.peercentrum.consensusprocess.ConsensusProcess;
import org.peercentrum.consensusprocess.ConsensusThreshold;
import org.peercentrum.consensusprocess.ProposedTransactions;
import org.peercentrum.consensusprocess.UniqueNodeList;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.H2PKDBSyncQuery;
import org.peercentrum.core.PB.H2PKProposedTransactions;
import org.peercentrum.core.PB.HashToPublicKeyMessage;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;

public class HashToPublicKeyApplication extends BaseApplicationMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashToPublicKeyApplication.class);
	public static final ApplicationIdentifier APP_ID=new ApplicationIdentifier(HashToPublicKeyApplication.class.getSimpleName().getBytes());
	ConsensusProcess consensus;
	HashToPublicKeyTXEvaluator txEvaluator;
	HashToPublicKeyDB db;
		
	public HashToPublicKeyApplication(NetworkServer nodeServer, HashToPublicKeyDB db, UniqueNodeList unl) throws Exception {
		super(nodeServer);
		if(unl==null){
			throw new NullPointerException("NULL UNL not allowed");
		}

		this.db=db;
		txEvaluator=new HashToPublicKeyTXEvaluator(db);
		final int DB_CLOSECYCLE_PERIOD_MS = 3000;
		ConsensusThreshold consensusThreshold=new ConsensusThreshold(50, 100, 10, DB_CLOSECYCLE_PERIOD_MS/5);
		consensus=new ConsensusProcess(nodeServer.getNodeIdentifier(), txEvaluator, unl, db, consensusThreshold) {
			@Override
			protected void broadcastProposedTransactions(ProposedTransactions ourProposedTx) {
				try {
					//TODO use generics instead of cast
					ProposedTransactions<HashToPublicKeyTransaction> proposedTX=ourProposedTx;
					LOGGER.debug("broadcasting our {} proposed transactions", proposedTX.size());
					PB.HashToPublicKeyMessage proposedTXMsg = encodeProposedTXToMsg(proposedTX);
					for(NodeIdentifier aValidator:unl){
						LOGGER.debug("Sending proposedTX to {}", aValidator);
						server.networkClient.sendRequest(aValidator, getApplicationId(), proposedTXMsg);
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
		dbCloseProcess.setName(super.server.getNodeIdentifier().toString());
		dbCloseProcess.start();
	}
	
	protected void bringLocalDBUptoDate() {
		try {
			//TODO We should ask each validator for the hash of the latest DB and compare
			//TODO Then, we can download the DB with the validated hash

			PB.HashToPublicKeyMessage.Builder appLevelMsg = PB.HashToPublicKeyMessage.newBuilder();
			PB.H2PKDBSyncQuery.Builder queryMsg=PB.H2PKDBSyncQuery.newBuilder();
			boolean requestFullSync=true;
			if(requestFullSync){
				queryMsg.setBeginDbVersionNumber(0);
			}
			appLevelMsg.setDbSyncQuery(queryMsg.build());
			NodeIdentifier remoteId=consensus.unl.getOneExcluding(this.server.getNodeIdentifier());
			HashToPublicKeyMessage appLevelResponse = server.networkClient.sendRequest(remoteId, getApplicationId(), appLevelMsg.build()).get();
			if(appLevelResponse.hasDbSyncResponse()){
				PB.H2PKDBSyncResponse dbSyncResponse = appLevelResponse.getDbSyncResponse();
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
			PB.HashToPublicKeyMessage.Builder appLevelResponseBuilder = PB.HashToPublicKeyMessage.newBuilder();

			PB.HashToPublicKeyMessage appRequest = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, PB.HashToPublicKeyMessage.class);
			if(appRequest.hasLocalTransaction()){
				LOGGER.debug("received local transaction");
				PB.HashToPublicKeyTransaction localTXMsg = appRequest.getLocalTransaction();
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

	protected ProposedTransactions<HashToPublicKeyTransaction> decodeMsgToProposedTX(PB.H2PKProposedTransactions proposedTxMsg) {
		UnsignedLong proposalDbVersion=UnsignedLong.valueOf(proposedTxMsg.getDbVersionNumber());
		NodeIdentifier senderPK=new NodeIdentifier(proposedTxMsg.getProposedBy().toByteArray());
		ProposedTransactions<HashToPublicKeyTransaction> proposedTransaction=new ProposedTransactions<HashToPublicKeyTransaction>(proposalDbVersion, senderPK);

		List<PB.HashToPublicKeyTransaction> txMsgList = proposedTxMsg.getProposedTransactionsList();
		for(PB.HashToPublicKeyTransaction txMsg:txMsgList){
			proposedTransaction.addTransactions(new HashToPublicKeyTransaction(txMsg));
		}
		return proposedTransaction;
	}

	protected HashToPublicKeyMessage encodeProposedTXToMsg(ProposedTransactions<HashToPublicKeyTransaction> proposedTX) {
		PB.H2PKProposedTransactions.Builder proposedTxMsg=PB.H2PKProposedTransactions.newBuilder();
		proposedTxMsg.setDbVersionNumber(proposedTX.getDbVersionNumber());
		proposedTxMsg.setProposedBy(ByteString.copyFrom(proposedTX.getFrom().getBytes()));
		for(HashToPublicKeyTransaction tx : proposedTX){
			proposedTxMsg.addProposedTransactions(tx.toMessage());
		}
		
		PB.HashToPublicKeyMessage.Builder applicationLevelBuilder = PB.HashToPublicKeyMessage.newBuilder(); 
		applicationLevelBuilder.setProposedTx(proposedTxMsg);
		return applicationLevelBuilder.build();
//		byte[] applicationLevelBytes=applicationLevelBuilder.build().toByteArray();
//		return Unpooled.copiedBuffer(applicationLevelBytes);
	}

	@Override
	public ApplicationIdentifier getApplicationId() {
		return APP_ID;
	}
}
