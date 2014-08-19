package org.peercentrum.dht;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import org.bouncycastle.util.Arrays;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.ProtobufByteBufCodec;
import org.peercentrum.core.Signature;
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

import com.google.common.primitives.UnsignedInts;
import com.google.protobuf.ByteString;

public class DHTApplication extends BaseApplicationMessageHandler {
  public static ApplicationIdentifier APP_ID=new ApplicationIdentifier(DHTApplication.class.getName().getBytes());
  private static final Logger LOGGER = LoggerFactory.getLogger(DHTApplication.class);
  private static final ByteBuf PONG_MESSAGE_BYTES;
  private static final String DHT_VALUE_TABLE_NAME = "dhtValue";
  private static final String NODES_TABLE_NAME = "nodes";
  private static final String INDEX_KEY = "indexKey";
  protected static final String NONCE_FIELD_NAME = "nonce";

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
//    ISqlJetCursor nodesCursor = nodesTable.open();
//    while(nodesCursor.eof()==false){
//      byte[] currentNodeId=nodesCursor.getBlobAsArray(0);
//      dhtClient.buckets.maybeAdd(new KIdentifier(currentNodeId));
//      nodesCursor.next();
//    }
//    nodesCursor.close();
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
      PB.DHTTopLevelMsg.Builder topLevelResponseMsg=PB.DHTTopLevelMsg.newBuilder();
      if(dhtTopLevelMsg.hasPing()){
        return new HeaderAndPayload(responseHeader, PONG_MESSAGE_BYTES);
      }
      else if(dhtTopLevelMsg.getFindCount()!=0){
        if(dhtTopLevelMsg.getFindCount()>1){
          LOGGER.warn("Multiple find not implemented yet, searching only for the first one");
        }
        handleFindMsg(topLevelResponseMsg, dhtTopLevelMsg.getFind(0));
        return new HeaderAndPayload(responseHeader, Unpooled.wrappedBuffer(topLevelResponseMsg.build().toByteArray()));
      }
      else if(dhtTopLevelMsg.getStoreValueCount()!=0){
        for(int i=0; i<dhtTopLevelMsg.getStoreValueCount(); i++){
          handleStoreMsg(topLevelResponseMsg, dhtTopLevelMsg.getStoreValue(i));
        }
        return new HeaderAndPayload(responseHeader, Unpooled.wrappedBuffer(topLevelResponseMsg.build().toByteArray()));
      }
    } catch (Exception e) {
      LOGGER.error("generateReponseFromQuery failed", e);
    }
    return null;
  }

  protected void handleFindMsg(PB.DHTTopLevelMsg.Builder topLevelResponseMsg, PB.DHTFindMsg findMsg) throws Exception {
    if(findMsg.hasKeyCriteria()==false){
      throw new Exception("Missing or conflicting search criteria in findMsg "+findMsg);
    }
    int numberOfNodesRequested=KBucket.K_BUCKET_SIZE;
    if(findMsg.hasNumberOfNodesRequested()){
      numberOfNodesRequested=findMsg.getNumberOfNodesRequested();
    }

    PB.DHTFoundMsg.Builder foundMsg=PB.DHTFoundMsg.newBuilder();
    final byte[] searchKey=findMsg.getKeyCriteria().toByteArray();
    byte[] valueFoundInDb=(byte[]) db.runReadTransaction(new ISqlJetTransaction() {
      @Override
      public Object run(SqlJetDb db) throws SqlJetException {
        ISqlJetCursor valuesCursor = dhtValueTable.lookup(INDEX_KEY, searchKey);
        byte[] valueFound=null;
        if(valuesCursor.eof()==false){
          valueFound=valuesCursor.getBlobAsArray("value");
        }
        valuesCursor.close();
        return valueFound;
      }
    });
    if(valueFoundInDb==null){
      populateClosestNodeTo(searchKey, numberOfNodesRequested, foundMsg);
    }
    else{
      foundMsg.setValue(ByteString.copyFrom(valueFoundInDb));
    }

    topLevelResponseMsg.addFound(foundMsg);
  }

  protected void populateClosestNodeTo(byte[] nodeIdToFind, int numberOfNodesRequested, PB.DHTFoundMsg.Builder foundMsg) {
    List<KIdentifier> closest=dhtClient.buckets.getClosestNodeTo(new KIdentifier(nodeIdToFind), numberOfNodesRequested);
    for(KIdentifier oneId : closest){
      PB.PeerEndpointMsg.Builder peerEndpointMsg=PB.PeerEndpointMsg.newBuilder();
      peerEndpointMsg.setIdentity(ByteString.copyFrom(oneId.getBytes()));

      InetSocketAddress socketAddress = server.getNodeDatabase().getEndpointByIdentifier(new NodeIdentifier(oneId.getBytes()));
      PB.PeerEndpointMsg.IPEndpointMsg.Builder ipEndpointBuilder=PB.PeerEndpointMsg.IPEndpointMsg.newBuilder();
      ipEndpointBuilder.setIpAddress(socketAddress.getHostName());
      ipEndpointBuilder.setPort(socketAddress.getPort());
      peerEndpointMsg.setIpEndpoint(ipEndpointBuilder);

      foundMsg.addClosestNodes(peerEndpointMsg);
    }
  }

  protected void handleStoreMsg(PB.DHTTopLevelMsg.Builder topLevelResponseMsg, PB.DHTStoreValueMsg storeValueMsg) throws Exception {
    if(storeValueMsg.hasSignature()==false){
      throw new Exception("The store message "+storeValueMsg+" does not have a signature");
    }
    if(storeValueMsg.hasKey()==false){
      throw new Exception("The store message "+storeValueMsg+" does not have a key");
    }
    if(storeValueMsg.hasValue()==false){
      throw new Exception("The store message "+storeValueMsg+" does not have a value");
    }
    final byte[] key=storeValueMsg.getKey().toByteArray();
    final byte[] value=storeValueMsg.getValue().toByteArray();
    if(key.length!=32){
      throw new Exception("The store message "+storeValueMsg+" does not have a key of proper length");
    }
    byte[] dataToVerify=Arrays.concatenate(key, value);
    Signature sig=new Signature(storeValueMsg.getSignature().toByteArray());
    NodeIdentifier signerId=new NodeIdentifier(key);
    if(sig.isSignatureValid(dataToVerify, signerId.getPublicKey())==false){
      throw new Exception("Failed to validate signature of message "+storeValueMsg);
    }
    
    if(storeValueMsg.hasNonce()==false){
      throw new Exception("Nonce is missing "+storeValueMsg);
    }
    final long nonceInMessage=UnsignedInts.toLong(storeValueMsg.getNonce());
    
    storeKeyValue(key, value, nonceInMessage);
    //TODO Add status message?
  }

  public void storeKeyValue(final byte[] key, final byte[] value, final long nonceInMessage) throws SqlJetException {
    db.runWriteTransaction(new ISqlJetTransaction() {
      @Override public Object run(SqlJetDb db) throws SqlJetException {
        ISqlJetCursor existingKeyCursor = dhtValueTable.lookup(INDEX_KEY, key);
        if(existingKeyCursor.eof()){
          dhtValueTable.insert(key, Long.valueOf(nonceInMessage), value);
        }
        else{
          long existingNonce=existingKeyCursor.getInteger(NONCE_FIELD_NAME);
          if(nonceInMessage<=existingNonce){
            LOGGER.warn("Already have an existing nonce {} . Nonce in message was {}", existingNonce, nonceInMessage);
          }
          else{
            existingKeyCursor.update(key, Long.valueOf(nonceInMessage), value);
          }
        }
        existingKeyCursor.close();
        return null;
      }
    });
  }

  @Override
  public ApplicationIdentifier getApplicationId() {
    return APP_ID;
  }

  private void createSchema() throws SqlJetException {
    db.createTable("CREATE TABLE "+DHT_VALUE_TABLE_NAME+"(key BLOB, nonce INTEGER, value BLOB);");
    db.createIndex("CREATE UNIQUE INDEX "+INDEX_KEY+" ON "+DHT_VALUE_TABLE_NAME+"(key)");
    db.createTable("CREATE TABLE "+NODES_TABLE_NAME+"(nodeId BLOB);");
  }

  public DHTClient getDHTClient() {
    return this.dhtClient;
  }

}