package org.peercentrum.blob;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;

import org.peercentrum.h2pk.HashIdentifier;
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

	public P2PBlobRepositoryFS(File repositoryDirectory) throws Exception {
		this.repositoryDirectory=repositoryDirectory;
		if(repositoryDirectory.exists()==false){
			throw new IOException("Repository "+repositoryDirectory.toPath().toAbsolutePath()+" does not exits");
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
	
	public HashIdentifier importFileIntoRepository(final File nonRepositoryFile) throws Exception {
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
						nonRepositoryFile.length(),
						P2PBlobApplication.DEFAULT_BLOCK_SIZE);
				LOGGER.debug("Added topHash/hashList={}", newRow);
				return null;
			}
		};
		db.runWriteTransaction(insertTx);
		concatenatedHashes.release();

		File hashedFileName=new File(nonRepositoryFile.getParentFile(), topHash.toString()+".blob");
		if(nonRepositoryFile.renameTo(hashedFileName)==false){
			throw new Exception("failed to rename "+nonRepositoryFile+" to "+hashedFileName);
		}
		return topHash;
	}

	protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
		if(schemaNeedsToBeCreated){
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			db.createTable("create table "+BLOB_META_TABLE_NAME+"("
					+ "blobHash BLOB, "
					+ "blocksHash BLOB, "
					+ "numberOfBlocks INTEGER, "
					+ "rangeLocalBlock TEXT, "
					+ "blobByteSize INTEGER, "
          + "blockByteSize INTEGER "
					+ ");");
			db.createIndex("CREATE INDEX " + BLOB_META_BLOBHASH_IDX + " ON "+BLOB_META_TABLE_NAME+"(blobHash)");
			db.commit();
		}
		blobMetaTable = db.getTable(BLOB_META_TABLE_NAME);
	}

	@Override
	public P2PBlobStoredBlob getStoredBlob(final HashIdentifier blobId) throws Exception{
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
          int blockByteSize=(int) blobMetaCursor.getInteger("blockByteSize");
					String localBlockRangeStr=blobMetaCursor.getString("rangeLocalBlock");
					P2PBlobRangeSet localBlockRange=null;
					if(localBlockRangeStr!=null){
						localBlockRange=new P2PBlobRangeSet(localBlockRangeStr);
					}
					P2PBlobHashList hashList=new P2PBlobHashList(blobMetaCursor.getBlobAsArray("blocksHash"));
					File blobFile = getBlobFile(blobId);
          P2PBlobStoredBlobRepositoryFS status=new P2PBlobStoredBlobRepositoryFS(blobFile,blobId, hashList, localBlockRange, blobByteSize, blockByteSize);
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

  @Override
  public P2PBlobStoredBlob createStoredBlob(P2PBlobHashList hashList, long blobLength, int blockSize) throws Exception {
    P2PBlobStoredBlob theBlob=getStoredBlob(hashList.getTopLevelHash());
    if(theBlob!=null){
      return theBlob;
    }
    File blobFile = getBlobFile(hashList.getTopLevelHash());
    theBlob=new P2PBlobStoredBlobRepositoryFS(blobFile , hashList.getTopLevelHash(), hashList, 
        new P2PBlobRangeSet(), blobLength, blockSize);
    insertStoredBlobMetaData(theBlob);
    return theBlob;
  }


  protected void insertStoredBlobMetaData(final P2PBlobStoredBlob theBlob) throws SqlJetException {
    final P2PBlobBlockLayout blockLayout = theBlob.getBlockLayout();
    final ByteBuf concatenatedHashes=Unpooled.buffer(blockLayout.getNumberOfBlocks()*P2PBlobHashList.HASH_BYTE_SIZE);
    for(HashIdentifier hashBlock:theBlob.getHashList()){
      concatenatedHashes.writeBytes(hashBlock.getBytes());
    }
    
    ISqlJetTransaction insertTx=new ISqlJetTransaction() {
      @Override public Object run(SqlJetDb db) throws SqlJetException {
        long newRow = blobMetaTable.insert(
            theBlob.getHashList().getTopLevelHash().getBytes(), 
            concatenatedHashes.array(),
            blockLayout.getNumberOfBlocks(),
            null,
            blockLayout.getLengthOfBlob(),
            blockLayout.getLengthOfEvenBlock());
        LOGGER.debug("Added topHash/hashList={}", newRow);
        return null;
      }
    };
    db.runWriteTransaction(insertTx);
    concatenatedHashes.release();
  }

  private File getBlobFile(final HashIdentifier blobId) {
    File blobFile=new File(P2PBlobRepositoryFS.this.repositoryDirectory, blobId.toString()+".blob");
    return blobFile;
  }

}
