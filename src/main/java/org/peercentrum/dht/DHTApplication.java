package org.peercentrum.dht;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.util.List;

import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.DHTFindMsg;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.HeaderAndPayload;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import com.google.protobuf.ByteString;

public class DHTApplication extends BaseApplicationMessageHandler {
  public static ApplicationIdentifier APP_ID=new ApplicationIdentifier(DHTApplication.class.getName().getBytes());
  private static final Logger LOGGER = LoggerFactory.getLogger(DHTApplication.class);
  private static final ByteBuf PONG_MESSAGE_BYTES;
  private static final String DHT_VALUE_TABLE_NAME = "dhtValue";
  private static final String NODES_TABLE_NAME = "nodes";
  private static final String INDEX_KEY = "indexKey";

  protected DHTClient dhtClient;
  protected SqlJetDb db;
  protected ISqlJetTable dhtValueTable;
  protected ISqlJetTable nodesTable;
  
  public DHTApplication(NetworkServer server) throws Exception {
    super(server);
    dhtClient=new DHTClient(server.networkClient);
    File serverFile=server.getConfig().getFile("DHT.db");
    boolean dbExists=serverFile.exists();
    db = new SqlJetDb(serverFile, true);
    db.open();
    if(dbExists==false){
      createSchema();
    }
    dhtValueTable=db.getTable(DHT_VALUE_TABLE_NAME);
    nodesTable=db.getTable(NODES_TABLE_NAME);

    //TODO Or load the kBuckets from server.getNodeDatabase() ?
    ISqlJetCursor nodesCursor = nodesTable.open();
    while(nodesCursor.eof()==false){
      byte[] currentNodeId=nodesCursor.getBlobAsArray(0);
      dhtClient.buckets.maybeAdd(new KIdentifier(currentNodeId));
      nodesCursor.next();
    }
    nodesCursor.close();
  }

  static {
    PB.DHTTopLevelMsg.Builder topLevelMsg=PB.DHTTopLevelMsg.newBuilder();
    topLevelMsg.setPing(PB.DHTPingMsg.newBuilder());
    PONG_MESSAGE_BYTES=Unpooled.wrappedBuffer(topLevelMsg.build().toByteArray());
  }
  
  @Override
  public HeaderAndPayload generateReponseFromQuery(ChannelHandlerContext ctx, HeaderAndPayload receivedMessage) {
    try {
      PB.DHTTopLevelMsg dhtTopLevelMsg = ProtobufByteBufCodec.decodeNoLengthPrefix(receivedMessage.payload, PB.DHTTopLevelMsg.class);
      dhtClient.receivedMessageFrom(new KIdentifier(server.getRemoteNodeIdentifier(ctx).getBytes()));
      
      PB.HeaderMsg.Builder responseHeader = super.newResponseHeaderForRequest(receivedMessage);
      if(dhtTopLevelMsg.hasPing()){
        return new HeaderAndPayload(responseHeader, PONG_MESSAGE_BYTES);
      }
      else if(dhtTopLevelMsg.hasFind()){
        return handleFindMsg(responseHeader, dhtTopLevelMsg.getFind());
      }
      else if(dhtTopLevelMsg.hasStoreValue()){
        return handleStoreMsg(responseHeader, dhtTopLevelMsg.getStoreValue());
      }
    } catch (Exception e) {
      LOGGER.error("generateReponseFromQuery failed", e);
    }
    return null;
  }

  protected HeaderAndPayload handleFindMsg(PB.HeaderMsg.Builder responseHeader, DHTFindMsg findMsg) throws Exception {
    if(findMsg.hasKeyCriteria()==findMsg.hasNodeCriteria()){
      throw new Exception("Missing or conflicting search criteria in findMsg "+findMsg);
    }
    int numberOfNodesRequested=3;
    if(findMsg.hasNumberOfNodesRequested()){
      numberOfNodesRequested=findMsg.getNumberOfNodesRequested();
    }

    PB.DHTFoundMsg.Builder foundMsg=PB.DHTFoundMsg.newBuilder();
    if(findMsg.hasKeyCriteria()){
      byte[] searchKey=findMsg.getKeyCriteria().toByteArray();
      ISqlJetCursor valuesCursor = dhtValueTable.lookup(DHT_VALUE_TABLE_NAME, searchKey);
      if(valuesCursor.eof()==false){
        byte[] valueFound=valuesCursor.getBlobAsArray(0);
        foundMsg.setValue(ByteString.copyFrom(valueFound));
      }
      else{
        populateClosestNodeTo(searchKey, numberOfNodesRequested, foundMsg);
      }
      valuesCursor.close();
    }
    else if(findMsg.hasNodeCriteria()){
      byte[] nodeSearched=findMsg.getNodeCriteria().toByteArray();
      populateClosestNodeTo(nodeSearched, numberOfNodesRequested, foundMsg);
    }

    ByteBuf findResponseBytes=Unpooled.wrappedBuffer(foundMsg.build().toByteArray());
    return new HeaderAndPayload(responseHeader, findResponseBytes);
  }

  protected void populateClosestNodeTo(byte[] nodeIdToFind, int numberOfNodesRequested, PB.DHTFoundMsg.Builder foundMsg) {
    List<KIdentifier> closest=dhtClient.getClosestNodeTo(new KIdentifier(nodeIdToFind), numberOfNodesRequested);
    for(KIdentifier oneId : closest){
      foundMsg.addClosestNodes(ByteString.copyFrom(oneId.getBytes()));
    }
  }

  protected HeaderAndPayload handleStoreMsg(PB.HeaderMsg.Builder responseHeader, final PB.DHTStoreValueMsg storeValueMsg) throws Exception {
    ISqlJetTransaction insertTx=new ISqlJetTransaction() {
      @Override public Object run(SqlJetDb db) throws SqlJetException {
        byte[] key=storeValueMsg.getKey().toByteArray();
        byte[] value=storeValueMsg.getValue().toByteArray();
        dhtValueTable.insert(key, value);
        return null;
      }
    };
    db.runWriteTransaction(insertTx);
    return new HeaderAndPayload(responseHeader, Unpooled.EMPTY_BUFFER);
  }

  @Override
  public ApplicationIdentifier getApplicationId() {
    return APP_ID;
  }

  private void createSchema() throws SqlJetException {
    db.createTable("CREATE TABLE "+DHT_VALUE_TABLE_NAME+"(key BLOB, value BLOB);");
    db.createIndex("CREATE INDEX "+INDEX_KEY+" ON "+DHT_VALUE_TABLE_NAME+"(key)");
    db.createTable("CREATE TABLE "+NODES_TABLE_NAME+"(nodeId BLOB);");
  }

}