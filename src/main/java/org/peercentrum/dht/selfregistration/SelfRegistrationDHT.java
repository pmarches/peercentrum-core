package org.peercentrum.dht.selfregistration;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.Arrays;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.Signature;
import org.peercentrum.dht.DHTApplication;
import org.peercentrum.dht.KBucket;
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

/**
 * This application allows a node to register itself with a chosen hash. No node 'owns' any key, but
 * a node cannot be registered/unregistered by anyone else.
 *
 * Usefull for stuff like swarm tracking like bitorrent.
 */
public class SelfRegistrationDHT extends DHTApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(SelfRegistrationDHT.class);
  public static ApplicationIdentifier APP_ID=new ApplicationIdentifier(SelfRegistrationDHT.class.getName().getBytes());
  private static final String DHT_VALUE_TABLE_NAME = "dhtValue";
  private static final String NODES_TABLE_NAME = "nodes";
  private static final String INDEX_KEY = "indexKey";
  protected static final String NONCE_FIELD_NAME = "nonce";
  
  protected SqlJetDb db;
  protected ISqlJetTable dhtValueTable;

  public SelfRegistrationDHT(NetworkServer server) throws Exception {
    super(server);
    setEntryTimeToLive(1, TimeUnit.DAYS);
    setEntryMaximumCardinality(1000);
    setEntryOverflowHandling(OverflowHandling.LIFO); //LIFO or FIFO    

    File serverFile=server.getConfig().getFile("DHT.db");
    boolean dbExists=serverFile.exists();
    db = new SqlJetDb(serverFile, true);
    db.open();
    if(dbExists==false){
      createSchema();
    }
    dhtValueTable=db.getTable(DHT_VALUE_TABLE_NAME);
  }

  public void storeKeyValue(final byte[] key, final byte[] value, final long nonceInMessage) throws SqlJetException {
    //FIXME Need to store the appID or have a different table per DHT app
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

  private void createSchema() throws SqlJetException {
    db.createTable("CREATE TABLE "+DHT_VALUE_TABLE_NAME+"(key BLOB, nonce INTEGER, value BLOB);");
    db.createIndex("CREATE UNIQUE INDEX "+INDEX_KEY+" ON "+DHT_VALUE_TABLE_NAME+"(key)");
    db.createTable("CREATE TABLE "+NODES_TABLE_NAME+"(nodeId BLOB);");
  }

  @Override
  public ApplicationIdentifier getApplicationId() {
    return APP_ID;
  }

  @Override
  public boolean isTransactionValid(PB.DHTStoreValueMsg storeValueMsg) {

    try {
      byte[] key=null;
      byte[] value=null;
      byte[] dataToVerify=Arrays.concatenate(key, value);
      Signature sig=new Signature(storeValueMsg.getSignature().toByteArray());
      NodeIdentifier signerId=new NodeIdentifier(key);
      if(sig.isSignatureValid(dataToVerify, signerId.getPublicKey())==false){
        LOGGER.error("Failed to validate signature of message {}", storeValueMsg);
        return false;
      }
    } catch (Exception e) {
      LOGGER.error("", e);
      return false;
    }
    return true;
  }

  @Override
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

  @Override
  protected void handleValidStoreMsg(PB.DHTTopLevelMsg.Builder topLevelResponseMsg, PB.DHTStoreValueMsg storeValueMsg) throws Exception {
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
    if(storeValueMsg.hasNonce()==false){
      throw new Exception("Nonce is missing "+storeValueMsg);
    }
    final long nonceInMessage=UnsignedInts.toLong(storeValueMsg.getNonce());

    if(isTransactionValid(storeValueMsg)){
      storeKeyValue(key, value, nonceInMessage);
    }
    //TODO Add status message?
  }

}
