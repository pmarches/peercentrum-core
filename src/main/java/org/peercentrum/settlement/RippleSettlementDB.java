package org.peercentrum.settlement;
//
//import java.util.HashMap;
//
//import org.peercentrum.core.AbstractNodeBalanceTable;
//import org.peercentrum.core.NodeIdentifier;
//import org.json.simple.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.tmatesoft.sqljet.core.SqlJetException;
//import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
//import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
//import org.tmatesoft.sqljet.core.table.ISqlJetTable;
//import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
//import org.tmatesoft.sqljet.core.table.SqlJetDb;
//
//public class RippleSettlementDB extends AbstractNodeBalanceTable {
//	private static final Logger LOGGER = LoggerFactory.getLogger(RippleSettlementDB.class);
//
//	static final String RIPPLE_SETTLEMENT_SETTINGS_TN = "rippleSettlementSettings";
//	static final String LAST_SYNCHED_LEDGER_FN = "lastSynchedLedger";
//
//	static final String SETTLEMENT_METHOD_TN = "settlementMethod";
//	static final String RIPPLE_ADDRESS_FN = "rippleAddress";
//	static final String SETTLEMENT_METHOD_RIPPLE_ADDRESS_IN = "settlementMethodRippleAddressIdx";
//
//	RippleDaemonWebsocketConnection connection;
//	RippleAddress localAddress;
//
//	ISqlJetTable settingsTable;
//	ISqlJetTable settlementMethodTable;
//	long lastLedgerNumber;
//
//	Thread transactionMonitorThread=new Thread(){
//		@Override
//		public void run() {
//			JSONSubscribtionFeed txFeed = connection.getTransactionFeed();
//			while(Thread.interrupted()==false){
//				try {
//					JSONObject newJSONTX = txFeed.take();
//					RippleTransaction tx=RippleJSONSerializer.createTransactionFromJSON(newJSONTX);
//					if(tx instanceof RipplePaymentTransaction==false){
//						continue;
//					}
//					RipplePaymentTransaction paymentTx=(RipplePaymentTransaction) tx;
//					LOGGER.debug("received payment tx {} ", paymentTx);
//					updateNodeBalanceFromPayment(paymentTx);
//				} catch (InterruptedException e) {
//					break;
//				} catch (Exception e) {
//					LOGGER.error("Error processing transaction feed", e);
//				}
//			}
//		}
//	};
//
//
//	public RippleSettlementDB(RippleAddress localAddress, String rippleSettlementDbPath) throws Exception {
//		super(rippleSettlementDbPath, "rippleSettlement");
//		getLastLedgerNumberFromDB();
//
//		this.localAddress=localAddress; //Maybe this should go in the DB settings too? Eventually we may need to track multiple ripple addresses?
//		this.connection=new RippleDaemonWebsocketConnection(RippleDaemonWebsocketConnection.RIPPLE_SERVER_URL);
//	}
//	
//	public void startMonitoringTransactions() throws Exception{
//		connection.subscribeToTransactionOfAddress(localAddress.toString());
//		updateBalancesWithNewTransactionsFromNetwork();
//		transactionMonitorThread.start();
//	}
//
//	protected void updateNodeBalanceFromPayment(final RipplePaymentTransaction paymentTx) throws SqlJetException {
//		if(paymentTx.amount.isNative()==false){
//			LOGGER.error("Non XRP transaction of '{}' is currently not supported. Tx {} will be ignored", paymentTx.amount, paymentTx.txHash);
//			//FIXME We should log this TX ID somewhere
//			return;
//		}
//
//		final NodeIdentifier nodeIdOfPayer = getNodeIdFromRippleAddress(paymentTx.payer);
//		if(nodeIdOfPayer==null){
//			LOGGER.warn("The TX {} will be ignored since we do not have the nodeId corresponding to rippleAddress {}", paymentTx.txHash, paymentTx.payer);
//			return;
//		}
//		long xrpDropsPaymentAmount=paymentTx.amount.toNativeDrops();
//		super.creditNode(nodeIdOfPayer, xrpDropsPaymentAmount);
//	}
//
//	protected void updateBalancesWithNewTransactionsFromNetwork() throws Exception {
//		final RippleTransactionHistory newTXSinceLastSync = connection.getTransactionsForAccount(localAddress.toString(), lastLedgerNumber);
//		newTXSinceLastSync.filterInPaymentsOnly();
//
//		db.runWriteTransaction(new ISqlJetTransaction() {
//			@Override public Object run(SqlJetDb db) throws SqlJetException {
//				long newLastSynchronizedLedger=lastLedgerNumber;
//
//				for(int i=0; i<newTXSinceLastSync.size(); i++){
//					RipplePaymentTransaction paymentTx=(RipplePaymentTransaction) newTXSinceLastSync.get(i);
//					updateNodeBalanceFromPayment(paymentTx);
//					newLastSynchronizedLedger=Math.max(newLastSynchronizedLedger, paymentTx.getLedgerIndex());
//				}
//				ISqlJetCursor settingsCursor = settingsTable.open();
//				settingsCursor.update(newLastSynchronizedLedger);
//				settingsCursor.close();
//				
//				return null;
//			}});
//	}
//
//	private NodeIdentifier getNodeIdFromRippleAddress(final RippleAddress rippleAddress) throws SqlJetException {
//		return (NodeIdentifier) db.runReadTransaction(new ISqlJetTransaction() {
//			@Override public Object run(SqlJetDb db) throws SqlJetException {
//				ISqlJetCursor rippleAddressCursor = settlementMethodTable.lookup(SETTLEMENT_METHOD_RIPPLE_ADDRESS_IN, rippleAddress.toString());
//				NodeIdentifier nodeId=null;
//				if(rippleAddressCursor.eof()==false){
//					nodeId=new NodeIdentifier(rippleAddressCursor.getBlobAsArray(NODE_ID_FN));
//				}
//				rippleAddressCursor.close();
//				return nodeId;
//			}
//		});
//	}
//
//	protected void getLastLedgerNumberFromDB() throws SqlJetException {
//		db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
//		ISqlJetCursor settingsCursor = settingsTable.open();
//		this.lastLedgerNumber=settingsCursor.getInteger(LAST_SYNCHED_LEDGER_FN);
//		settingsCursor.close();
//		db.commit();
//	}
//
//	@Override
//	protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
//		super.maybeCreateSchema(schemaNeedsToBeCreated);
//		if(schemaNeedsToBeCreated){
//			db.beginTransaction(SqlJetTransactionMode.WRITE);
//			db.createTable("create table "+SETTLEMENT_METHOD_TN+"("
//					+ NODE_ID_FN+" BLOB PRIMARY KEY NOT NULL, "
//					+ RIPPLE_ADDRESS_FN+" TEXT NOT NULL "
//					+ ");");
//			db.createIndex("CREATE INDEX "+SETTLEMENT_METHOD_RIPPLE_ADDRESS_IN+" on "+SETTLEMENT_METHOD_TN+"("+RIPPLE_ADDRESS_FN+")");
//
//			db.createTable("create table "+RIPPLE_SETTLEMENT_SETTINGS_TN+"("
//					+ LAST_SYNCHED_LEDGER_FN+" INTEGER NOT NULL "
//					+ ");");
//			settingsTable = db.getTable(RIPPLE_SETTLEMENT_SETTINGS_TN);
//			settingsTable.insert(RippleDaemonConnection.GENESIS_LEDGER_NUMBER);
//			db.commit();
//		}
//		settingsTable = db.getTable(RIPPLE_SETTLEMENT_SETTINGS_TN);
//		settlementMethodTable = db.getTable(SETTLEMENT_METHOD_TN);
//	}
//
//	public void setSettlementMethod(final NodeIdentifier nodeId, final RippleAddress rippleAddress) throws Exception {
//		db.runWriteTransaction(new ISqlJetTransaction() {
//			@Override public Object run(SqlJetDb db) throws SqlJetException {
//				ISqlJetCursor rippleAddressCursor = settlementMethodTable.lookup(SETTLEMENT_METHOD_RIPPLE_ADDRESS_IN, rippleAddress.toString());
//
//				HashMap<String, Object> record=new HashMap<>();
//				record.put(NODE_ID_FN, nodeId.getBytes());
//				record.put(RIPPLE_ADDRESS_FN, rippleAddress.toString());
//				if(rippleAddressCursor.eof()){
//					settlementMethodTable.insertByFieldNames(record);
//				}
//				else{
//					String oldRippleAddress = rippleAddressCursor.getString(RIPPLE_ADDRESS_FN);
//					if(oldRippleAddress.equals(rippleAddress.toString())==false){
//						LOGGER.warn("Node {} has changed it's ripple address from {} to {}", new Object[]{nodeId, oldRippleAddress, rippleAddress});
//					}
//					rippleAddressCursor.updateByFieldNames(record);
//				}
//				rippleAddressCursor.close();
//				return null;
//			}
//		});
//	}
//
//	@Override
//	public void close() throws Exception {
//		LOGGER.debug("Stopping monitoring thread");
//		transactionMonitorThread.interrupt();
//		transactionMonitorThread.join();
//		LOGGER.debug("Monitoring thread stopped");
//		super.close();
//	}
//}
