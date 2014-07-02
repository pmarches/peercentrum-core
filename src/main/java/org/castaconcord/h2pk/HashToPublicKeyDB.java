package org.castaconcord.h2pk;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.castaconcord.consensusprocess.ConsensusDB;
import org.castaconcord.core.ProtocolBuffer;
import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncResponse;
import org.castaconcord.core.ProtocolBuffer.H2PKDBSyncUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.schema.ISqlJetTableDef;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import com.google.common.primitives.UnsignedLong;
import com.google.protobuf.ByteString;

public class HashToPublicKeyDB<T> extends ConsensusDB<HashToPublicKeyTransaction> implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(HashToPublicKeyDB.class);

	protected static final String FIELD_PUBLIC_KEY = "publicKey";
	protected static final String INDEX_ADDRESS = "addressIdx";
	protected static final String INDEX_ADDRESS_PK = "addressPkIdx";
	protected SqlJetDb db;
	protected ISqlJetTable addressToPKTable;

	public HashToPublicKeyDB() {
		super(UnsignedLong.ONE); // FIXME Get the db version number from SQL
		try {
			db = new SqlJetDb(SqlJetDb.IN_MEMORY, true);
			db.open();
			maybeCreateSchema();
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}
	}

	public void registerPublicKey(BazarroHashIdentifier address, BazarroPublicKeyIdentifier publicKey) throws Exception {
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {
			long newRow = addressToPKTable.insert(address.getBytes(), publicKey.getBytes());
			LOGGER.debug("newRow={}", newRow);
		} finally {
			db.commit();
		}
	}

	public void unRegisterPublicKeyForAddress(BazarroHashIdentifier address, BazarroPublicKeyIdentifier publicKey)
			throws Exception {
		db.beginTransaction(SqlJetTransactionMode.WRITE);
		try {
			ISqlJetCursor deleteCursor = addressToPKTable.lookup(INDEX_ADDRESS_PK, address.getBytes(), publicKey.getBytes());
			if (deleteCursor.eof()) {
				LOGGER.warn("could not find address " + address + " and pk " + publicKey + " in the database.");
			} else {
				deleteCursor.delete();
			}
			deleteCursor.close();
		} finally {
			db.commit();
		}
	}

	public Set<BazarroPublicKeyIdentifier> getRegisteredPublicKeysForAddress(BazarroHashIdentifier address) {
		try {
			Set<BazarroPublicKeyIdentifier> publicKeys = new HashSet<>();

			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			try {
				ISqlJetCursor matchingkPKCursor = addressToPKTable.lookup(INDEX_ADDRESS, address.getBytes());
				while(!matchingkPKCursor.eof()){
					byte[] pkBytes=matchingkPKCursor.getBlobAsArray(FIELD_PUBLIC_KEY);
					publicKeys.add(new BazarroPublicKeyIdentifier(pkBytes));
					matchingkPKCursor.next();
				}
			} finally {
				db.commit();
			}

			return publicKeys;
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}
	}

	private void dumpToConsole() {
		try {
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			ISqlJetCursor cursor = addressToPKTable.order(INDEX_ADDRESS_PK);
			while(!cursor.eof()){
				System.err.println(DatatypeConverter.printHexBinary(cursor.getBlobAsArray(0))+" "+DatatypeConverter.printHexBinary(cursor.getBlobAsArray(1)));
				cursor.next();
			}
			cursor.close();
			db.commit();
		} catch (SqlJetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isPublicKeyRegisteredForAddress(BazarroHashIdentifier address, BazarroPublicKeyIdentifier publicKey) {
		if(address==null){
			throw new NullPointerException("address cannot be null");
		}
		if(publicKey==null){
			throw new NullPointerException("publicKey cannot be null");
		}
		
		try {
			boolean matchesFound;
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			try {
				ISqlJetCursor matchingkPKCursor = addressToPKTable.lookup(INDEX_ADDRESS_PK, address.getBytes(), publicKey.getBytes());
				matchesFound=matchingkPKCursor.next();
			} finally {
				db.commit();
			}
			return matchesFound;
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}
	}

	private void maybeCreateSchema() throws SqlJetException {
		final ISqlJetTableDef tDef = db.createTable("create table addressToPK(address BLOB, publicKey BLOB);");
		addressToPKTable = db.getTable(tDef.getName());
		db.createIndex("CREATE INDEX " + INDEX_ADDRESS + " ON addressToPK(address)");
		db.createIndex("CREATE UNIQUE INDEX " + INDEX_ADDRESS_PK + " ON addressToPK(address,publicKey)");
	}

	/**
	 * This applies the transaction regardless if it is valid or not.
	 * 
	 * @param tx
	 * @throws Exception
	 */
	@Override
	public void applyOneTransaction(HashToPublicKeyTransaction tx) throws Exception {
		BazarroHashIdentifier address = tx.getAddress();
		BazarroPublicKeyIdentifier publicKey = tx.getPublicKey();
		if (tx.isAppend()) {
			registerPublicKey(address, publicKey);
		} else if (tx.isRemove()) {
			unRegisterPublicKeyForAddress(address, publicKey);
		} else {
			LOGGER.error("unsupported operation " + tx);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			db.close();
		} catch (SqlJetException e) {
			throw new IOException(e);
		}
	}

	public void generateDbSyncResponse(ProtocolBuffer.H2PKDBSyncQuery dbSyncQuery, ProtocolBuffer.HashToPublicKeyMessage.Builder appLevelResponseBuilder) {
		H2PKDBSyncResponse.Builder dbSyncResponse=H2PKDBSyncResponse.newBuilder();
		dbSyncResponse.setLastDbVersionNumber(this.dbVersion.intValue());
		dbSyncResponse.setSyncDbVersionNumber(this.dbVersion.intValue());
//		dbSyncResponse.setLastDbHashValue(lastHashValue); //TODO

		if(dbSyncQuery.hasBeginDbVersionNumber()==false){
			//Query does not wish to receive data units at this time
			appLevelResponseBuilder.setDbSyncResponse(dbSyncResponse);
			return;
		}
		
		try {
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			try {
				ISqlJetCursor syncCursor = this.addressToPKTable.open();
				byte[] lastAddress=null;
				H2PKDBSyncUnit.Builder currentUnit=null;
				while(syncCursor.eof()==false){
					byte[] currentAddress=syncCursor.getBlobAsArray(0);
					byte[] currentPK=syncCursor.getBlobAsArray(1);
					boolean hasAddressChanged=!Arrays.equals(currentAddress, lastAddress);
					if(hasAddressChanged){
						lastAddress=currentAddress;
						if(currentUnit!=null){
							dbSyncResponse.addSyncUnits(currentUnit);
						}
						currentUnit = H2PKDBSyncUnit.newBuilder();
						currentUnit.setAddress(ByteString.copyFrom(currentAddress));
					}
					currentUnit.addPublicKeysRegistered(ByteString.copyFrom(currentPK));
					syncCursor.next();
				}
				if(currentUnit!=null){
					dbSyncResponse.addSyncUnits(currentUnit);
				}

				syncCursor.close();
			} finally {
				db.commit();
			}
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}

		appLevelResponseBuilder.setDbSyncResponse(dbSyncResponse);

	}

	public void integrateSyncUnit(H2PKDBSyncResponse dbSyncResponse) {
		if(dbSyncResponse.hasSyncDbVersionNumber()==false){
			throw new RuntimeException("Missing syncDbVersion field");
		}

		//FIXME We need to implement a delta sync mechanism.. Not this drop&insert
		try {
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			try {
				ISqlJetCursor deleteAllCursor = addressToPKTable.open();
				while(deleteAllCursor.eof()==false){
					deleteAllCursor.delete();
				}
				deleteAllCursor.close();

				for(ProtocolBuffer.H2PKDBSyncUnit syncUnit: dbSyncResponse.getSyncUnitsList()){
					byte[] address=syncUnit.getAddress().toByteArray();
					for(ByteString publicKey : syncUnit.getPublicKeysRegisteredList()){
						addressToPKTable.insert(address, publicKey.toByteArray());
					}
				}
				this.dbVersion=UnsignedLong.valueOf(dbSyncResponse.getSyncDbVersionNumber());
			} finally {
				db.commit();
			}
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}

	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null){
			return false;
		}
		HashToPublicKeyDB<T> otherDb=(HashToPublicKeyDB<T>) obj;
		if(dbVersion.equals(otherDb.dbVersion)==false){
			return false;
		}

		try {
			db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			otherDb.db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
			try {
				ISqlJetCursor thisCursor = addressToPKTable.order(INDEX_ADDRESS_PK);
				ISqlJetCursor otherCursor = otherDb.addressToPKTable.order(INDEX_ADDRESS_PK);
				while(true){
					if(thisCursor.eof() && otherCursor.eof()){
						break;
					}
					
					if(thisCursor.eof() != otherCursor.eof()){
						return false;
					}
					
					byte[] thisAddress=thisCursor.getBlobAsArray(0);
					byte[] otherAddress=otherCursor.getBlobAsArray(0);
					if(Arrays.equals(thisAddress, otherAddress)==false){
						return false;
					}
					
					byte[] thisPublicKey=thisCursor.getBlobAsArray(1);
					byte[] otherPublicKey=otherCursor.getBlobAsArray(1);
					if(Arrays.equals(thisPublicKey, otherPublicKey)==false){
						return false;
					}
					
					thisCursor.next();
					otherCursor.next();
				}
			} finally {
				db.commit();
				otherDb.db.commit();
			}
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}

		return true;
	}
}
