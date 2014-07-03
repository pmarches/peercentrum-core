package org.peercentrum.core;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class AbstractNodeBalanceDB implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNodeBalanceDB.class);
	protected static final String NODE_ID_FN = "nodeId";
	protected static final String BALANCE_FN = "balance";
	
	protected String balanceTN;
	protected SqlJetDb db;
	protected ISqlJetTable balanceTable;
	protected long negativeBalanceAllowed=1;

	public AbstractNodeBalanceDB(String dbPath, String tableName) throws SqlJetException {
		balanceTN=tableName;
		synchronized(this){
			setupDatabase(dbPath);
		}
	}
	
	protected void setupDatabase(String dbPath) throws SqlJetException {
		boolean schemaNeedsToBeCreated;
		if(dbPath==null){
			schemaNeedsToBeCreated=true;
			db = new SqlJetDb(SqlJetDb.IN_MEMORY, true);
		}
		else{
			File dbFile=new File(dbPath);
			schemaNeedsToBeCreated=dbFile.exists()==false;
			db = new SqlJetDb(dbFile, true);
		}
		db.open();
		maybeCreateSchema(schemaNeedsToBeCreated);
	}

	protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
		if(schemaNeedsToBeCreated){
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			db.createTable("create table "+balanceTN+"("
					+ NODE_ID_FN+" BLOB PRIMARY KEY NOT NULL, "
					+ BALANCE_FN+" INTEGER NOT NULL "
					+ ");");
			db.commit();
		}
		balanceTable = db.getTable(balanceTN);
	}

	public long getBalanceForNode(final NodeIdentifier nodeId) throws SqlJetException {
		ISqlJetTransaction getBalanceTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetCursor nodeAccountCursor = balanceTable.lookup(null, nodeId.getBytes());
				long balance=0;
				if(nodeAccountCursor.eof()==false){
					balance=nodeAccountCursor.getInteger(BALANCE_FN);
				}
				nodeAccountCursor.close();
				return balance;
			}
		};
		return (long) db.runReadTransaction(getBalanceTx);
	}

	public boolean maybeDebit(final NodeIdentifier nodeId, final long amountToBeDebited) {
		if(amountToBeDebited<0){
			throw new RuntimeException("Invalid debit amount "+amountToBeDebited);
		}
		
		ISqlJetTransaction maybeDebitNodeTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				boolean isDebitAllowed=false;
				ISqlJetCursor nodeBalanceCursor = balanceTable.lookup(null, nodeId.getBytes());
				if(nodeBalanceCursor.eof()){
					if(negativeBalanceAllowed<amountToBeDebited){
						LOGGER.info("Refused to debit {} because the autoloan amount is only {}", amountToBeDebited, negativeBalanceAllowed);
						isDebitAllowed=false;
					}
					else{
						LOGGER.info("First-time debit of {} allowed for node '{}' ", amountToBeDebited, nodeId);
						balanceTable.insert(nodeId.getBytes(), -amountToBeDebited);
						isDebitAllowed=true;
					}
				}
				else{
					long oldBalance=nodeBalanceCursor.getInteger(BALANCE_FN);
					long newBalance=oldBalance-amountToBeDebited;
					if(newBalance<0 && -newBalance>negativeBalanceAllowed){
						LOGGER.info("Debit denied to node '{}' because the resulting balance would have been {}", nodeId, newBalance);
						isDebitAllowed=false;
					}
					else{
						LOGGER.info("Debit allowed to node '{}' new balance {}", nodeId, newBalance);
						LOGGER.debug("Old balance: {} - debit {}", oldBalance, amountToBeDebited);

						Map<String, Object> fieldsToUpdate=new HashMap<>();
						fieldsToUpdate.put(BALANCE_FN, newBalance);
						nodeBalanceCursor.updateByFieldNames(fieldsToUpdate);
						isDebitAllowed=true;
					}
				}
				nodeBalanceCursor.close();
				return isDebitAllowed;
			}
		};
		
		try {
			return (boolean) db.runWriteTransaction(maybeDebitNodeTx);
		} catch (SqlJetException e) {
			LOGGER.error("db error", e);
			throw new RuntimeException(e);
		}
	}

	public long creditNode(final NodeIdentifier nodeId, final long creditAmount) {
		if(creditAmount<0){
			throw new RuntimeException("Invalid credit amount "+creditAmount);
		}

		ISqlJetTransaction creditNodeTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetCursor nodeBalanceCursor = balanceTable.lookup(null, nodeId.getBytes());
				long newBalance;
				if(nodeBalanceCursor.eof()){
					LOGGER.info("First time credit of {} for node '{}'", creditAmount, nodeId);
					balanceTable.insert(nodeId.getBytes(), creditAmount);
					newBalance=creditAmount;
				}
				else{
					Map<String, Object> fieldsToUpdate=new HashMap<>();
					long oldBalance=nodeBalanceCursor.getInteger(BALANCE_FN);
					newBalance=oldBalance+creditAmount;
					LOGGER.info("Node '{}' has new balance of {}", nodeId, newBalance);
					fieldsToUpdate.put(BALANCE_FN, newBalance);
					nodeBalanceCursor.updateByFieldNames(fieldsToUpdate);
				}
				nodeBalanceCursor.close();
				return newBalance;
			}
		};
		try {
			return (long) db.runWriteTransaction(creditNodeTx);
		} catch (SqlJetException e) {
			LOGGER.error("db error", e);
			throw new RuntimeException(e);
		}
	}
	
	synchronized public void getAllBalances(){
		System.out.println("-------BEGIN ALL BALANCES---------");
		try {
			ISqlJetCursor nodeAccountCursor=null;
			try {
				db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
				nodeAccountCursor = balanceTable.scope(null, null, null);
				while(nodeAccountCursor.eof()==false){
					NodeIdentifier node=new NodeIdentifier(nodeAccountCursor.getBlobAsArray(NODE_ID_FN));
					System.out.println(nodeAccountCursor.getInteger(BALANCE_FN)+" '"+node+"'");
					nodeAccountCursor.next();
				}
			}
			finally{
				if(nodeAccountCursor!=null){
					nodeAccountCursor.close();
				}
				db.commit();
				System.out.println("-------END ALL BALANCES---------");
			}
		} catch (SqlJetException e) {
			LOGGER.error("db error", e);
			throw new RuntimeException(e);
		}
	}

	public void setDefaultLoanAllowed(long defaultLoanAmmount) {
		this.negativeBalanceAllowed=defaultLoanAmmount;
	}


	@Override
	public void close() throws Exception {
		db.close();
	}

}
