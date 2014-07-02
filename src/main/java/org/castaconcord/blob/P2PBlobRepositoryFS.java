package org.castaconcord.blob;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;

import org.castaconcord.h2pk.HashIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class P2PBlobRepositoryFS extends P2PBlobRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobRepositoryFS.class);

	protected static final String BLOB_META_TABLE_NAME = "blobMeta";
	protected static final String BLOB_META_BLOBHASH_IDX = "blobHashToMetaIdx";
	protected static final String REPOSITORY_DB_FILENAME = "repoDatabase.blob";

	protected SqlJetDb db;
	protected ISqlJetTable blobMetaTable;
	protected File repositoryDirectory;
	protected Hashtable<HashIdentifier, P2PBlobStoredBlobRepositoryFS> cachedTransitStatus=new Hashtable<>();

	private FilenameFilter nonRepositoryFilenameFilter=new FilenameFilter() {
		@Override
		public boolean accept(File arg0, String arg1) {
			return !arg1.endsWith(".blob") && arg1.equals(REPOSITORY_DB_FILENAME)==false;
		}
	};

	public P2PBlobRepositoryFS(Path repositoryPath) throws Exception {
		this.repositoryDirectory=repositoryPath.toFile();
		if(repositoryDirectory.exists()==false){
			throw new IOException("Repository "+repositoryPath.toAbsolutePath()+" does not exits");
		}
		
		try {
			File dbFile=new File(repositoryDirectory, REPOSITORY_DB_FILENAME);
			boolean schemaNeedsToBeCreated=dbFile.exists()==false;
			db = new SqlJetDb(dbFile, true);
			db.open();
			maybeCreateSchema(schemaNeedsToBeCreated);
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}
		
		for(File nonRepositoryFile:repositoryDirectory.listFiles(nonRepositoryFilenameFilter)){
			importFileIntoRepository(nonRepositoryFile);
		}

	}
	
	protected void importFileIntoRepository(final File nonRepositoryFile) throws Exception {
		final P2PBlobHashList fileHashList=P2PBlobHashList.createFromFile(nonRepositoryFile);
		final HashIdentifier topHash=fileHashList.getTopLevelHash();
		final ByteBuf concatenatedHashes=Unpooled.buffer(fileHashList.size()*P2PBlobHashList.HASH_BYTE_SIZE);
		for(HashIdentifier hashBlock:fileHashList){
			concatenatedHashes.writeBytes(hashBlock.getBytes());
		}
		
		ISqlJetTransaction insertTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				long newRow = blobMetaTable.insert(
						topHash.getBytes(), 
						concatenatedHashes.array(),
						fileHashList.size(),
						null,
						nonRepositoryFile.length());
				LOGGER.debug("Added topHash/hashList={}", newRow);
				return null;
			}
		};
		db.runWriteTransaction(insertTx);
		concatenatedHashes.release();

		File hashedFileName=new File(nonRepositoryFile.getParentFile(), topHash.toString()+".blob");
		if(nonRepositoryFile.renameTo(hashedFileName)==false){
			LOGGER.error("failed to rename "+nonRepositoryFile+" to "+hashedFileName);
		}
	}

	protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
		if(schemaNeedsToBeCreated){
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			db.createTable("create table "+BLOB_META_TABLE_NAME+"("
					+ "blobHash BLOB, "
					+ "blocksHash BLOB, "
					+ "numberOfBlocks INTEGER, "
					+ "rangeLocalBlock TEXT, "
					+ "blobByteSize INTEGER "
					+ ");");
			db.createIndex("CREATE INDEX " + BLOB_META_BLOBHASH_IDX + " ON "+BLOB_META_TABLE_NAME+"(blobHash)");
			db.commit();
		}
		blobMetaTable = db.getTable(BLOB_META_TABLE_NAME);
	}

	@Override
	public P2PBlobStoredBlob getStoredBlob(final HashIdentifier blobId){
		synchronized(cachedTransitStatus){
			P2PBlobStoredBlobRepositoryFS transitStatus = cachedTransitStatus.get(blobId);
			if(transitStatus!=null){
				return transitStatus;
			}

			ISqlJetTransaction getStoredBlockTx=new ISqlJetTransaction() {
				@Override public Object run(SqlJetDb db) throws SqlJetException {
					ISqlJetCursor blobMetaCursor = blobMetaTable.lookup(BLOB_META_BLOBHASH_IDX, blobId.getBytes());
					if(blobMetaCursor.eof()){
						return null;
					}
					long blobByteSize=blobMetaCursor.getInteger("blobByteSize");
					String localBlockRangeStr=blobMetaCursor.getString("rangeLocalBlock");
					P2PBlobRangeSet localBlockRange=null;
					if(localBlockRangeStr!=null){
						localBlockRange=new P2PBlobRangeSet(localBlockRangeStr);
					}
					P2PBlobHashList hashList=new P2PBlobHashList(blobMetaCursor.getBlobAsArray("blocksHash"));
					P2PBlobStoredBlobRepositoryFS status=new P2PBlobStoredBlobRepositoryFS(blobId, hashList, localBlockRange, blobByteSize, P2PBlobRepositoryFS.this);
					cachedTransitStatus.put(blobId, status);
					return status;
				}
			};

			try {
				return (P2PBlobStoredBlob) db.runReadTransaction(getStoredBlockTx);
			} catch (SqlJetException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

}
