package org.peercentrum.core;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class NodeDatabase implements AutoCloseable {
	private static final Logger LOGGER = LoggerFactory.getLogger(NodeDatabase.class);

	static final String NODE_INFO_TABLE_NAME = "NODE_INFO";

	static final String NODE_APPLICATION_TABLE_NAME = "NODE_APPLICATION";

//	static final String NODE_APPLICATION_INDEX_NAME = "NODE_ID_APP";
	static final String ENDPOINT_ADDRESS_FN = "endPointAddress";
	static final String ENDPOINT_PORT_FN = "endPointPort";
	static final String NODE_ID_FN = "nodeId";

	
//	Hashtable<NodeIdentifier, NodeInformation> idToPeer=new Hashtable<>();
	protected SqlJetDb db;

	ISqlJetTable nodeInfoTable;
	ISqlJetTable nodeApplicationTable;
	
	public NodeDatabase(File nodeDatabasePath) {
		try {
			boolean schemaNeedsToBeCreated;
			if(nodeDatabasePath==null){
				schemaNeedsToBeCreated=true;
				db = new SqlJetDb(SqlJetDb.IN_MEMORY, true);
			}
			else{
				schemaNeedsToBeCreated=nodeDatabasePath.exists()==false;
				db = new SqlJetDb(nodeDatabasePath, true);
			}
			db.open();
			maybeCreateSchema(schemaNeedsToBeCreated);
		} catch (SqlJetException e) {
			throw new RuntimeException(e);
		}
	}

	private void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
		if(schemaNeedsToBeCreated){
			db.beginTransaction(SqlJetTransactionMode.WRITE);
			db.createTable("create table "+NODE_INFO_TABLE_NAME+"("
					+ NODE_ID_FN+" BLOB PRIMARY KEY NOT NULL, "
					+ ENDPOINT_ADDRESS_FN+" TEXT NOT NULL, "
					+ ENDPOINT_PORT_FN+" INTEGER NOT NULL "
					+ ");");

			db.createTable("create table "+NODE_APPLICATION_TABLE_NAME+"("
					+ NODE_ID_FN+" BLOB, "
					+ "applicationId BLOB "
					+ ");");
			//TODO Maybe the index needs to be on application instead of nodeId
//			db.createIndex("CREATE INDEX " + NODE_APPLICATION_INDEX_NAME + " ON "+NODE_APPLICATION_TABLE_NAME+"(nodeId)");
			db.commit();
		}
		nodeInfoTable = db.getTable(NODE_INFO_TABLE_NAME);
		nodeApplicationTable = db.getTable(NODE_APPLICATION_TABLE_NAME);
	}

	//TODO Add some checks to ensure we do not map stale IP/Port info.. 
	public void mapNodeIdToAddress(final NodeIdentifier NodeIdentifier, final InetSocketAddress nodeSocketAddress) {
//		NodeInformation info = new NodeInformation(NodeIdentifier, nodeSocketAddress);
//		LOGGER.debug("Mapped {} to {}", NodeIdentifier, nodeSocketAddress);
//		idToPeer.put(NodeIdentifier, info);
		ISqlJetTransaction updateEndpointTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				ISqlJetCursor nodeInfoCursor = nodeInfoTable.lookup(null, NodeIdentifier.getBytes());
				if(nodeInfoCursor.eof()){
					nodeInfoTable.insert(NodeIdentifier.getBytes(), nodeSocketAddress.getHostString(), nodeSocketAddress.getPort());
				}
				else{
					Map<String, Object> fieldsToUpdate=new HashMap<>();
					fieldsToUpdate.put(ENDPOINT_ADDRESS_FN, nodeSocketAddress.getHostString());
					fieldsToUpdate.put(ENDPOINT_PORT_FN, nodeSocketAddress.getPort());
					nodeInfoCursor.updateByFieldNames(fieldsToUpdate);
				}
				nodeInfoCursor.close();
				return null;
			}
		};
		try {
			db.runWriteTransaction(updateEndpointTx);
		} catch (SqlJetException e) {
			LOGGER.error("Db error", e);
			throw new RuntimeException(e);
		}
	}

	public long size() {
		try {
			ISqlJetTransaction getCountTx=new ISqlJetTransaction() {
				@Override
				public Object run(SqlJetDb db) throws SqlJetException {
					ISqlJetCursor allRowsCursor = nodeInfoTable.scope(null, null, null);
					long count=allRowsCursor.getRowCount();
					allRowsCursor.close();
					return count;
				}
			};
			return (long) db.runReadTransaction(getCountTx);
		} catch (SqlJetException e) {
			LOGGER.error("Db error", e);
			throw new RuntimeException(e);
		}
	}

	public InetSocketAddress getEndpointByIdentifier(final NodeIdentifier remoteID) {
		try {
			ISqlJetTransaction getEndpointTx=new ISqlJetTransaction() {
				@Override public Object run(SqlJetDb db) throws SqlJetException {
					ISqlJetCursor nodeInfoCursor = nodeInfoTable.lookup(null, remoteID.getBytes());
					InetSocketAddress endpoint=null;
					if(nodeInfoCursor.eof()==false){
						endpoint=new InetSocketAddress(nodeInfoCursor.getString(ENDPOINT_ADDRESS_FN), 
								(int) nodeInfoCursor.getInteger(ENDPOINT_PORT_FN));
					}
					nodeInfoCursor.close();
					return endpoint;
				}
			};
			return (InetSocketAddress) db.runReadTransaction(getEndpointTx);
		} catch (SqlJetException e) {
			LOGGER.error("db error", e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public List<NodeInformation> getAllNodeInformation(final int maxNumberOfNodes){
		ISqlJetTransaction getAllNodesTx=new ISqlJetTransaction() {
			@Override public Object run(SqlJetDb db) throws SqlJetException {
				ArrayList<NodeInformation> listOfNodeInfo=new ArrayList<>();
				ISqlJetCursor allNodesCursor = nodeInfoTable.scope(nodeInfoTable.getPrimaryKeyIndexName(), null, null);
				for(int i=0; i<maxNumberOfNodes && allNodesCursor.eof()==false; i++){
					InetSocketAddress endPoint=new InetSocketAddress(allNodesCursor.getString(ENDPOINT_ADDRESS_FN), 
							(int) allNodesCursor.getInteger(ENDPOINT_PORT_FN));
					
					NodeIdentifier nodeId=new NodeIdentifier(allNodesCursor.getBlobAsArray(NODE_ID_FN));
					NodeInformation currentBNI=new NodeInformation(nodeId, endPoint);
					listOfNodeInfo.add(currentBNI);
					
					allNodesCursor.next();
				}
				allNodesCursor.close();
				return listOfNodeInfo;
			}
		};
		try {
			return (List<NodeInformation>) db.runReadTransaction(getAllNodesTx);
		} catch (SqlJetException e) {
			LOGGER.error("DB error", e);
			throw new RuntimeException(e);
		}
	}
//	@Override
//	public Iterator<NodeInformation> iterator() {
//		return idToPeer.values().iterator();
//	}

	@Override
	public void close() throws IOException {
		try {
			db.close();
		} catch (SqlJetException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
