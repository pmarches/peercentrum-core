package org.peercentrum.settlement;

import org.peercentrum.core.AbstractApplicationDB;
import org.peercentrum.core.AbstractApplicationTable;
import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.table.ISqlJetCursor;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.ISqlJetTransaction;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

import com.google.bitcoin.core.ECKey;

public class SettlementMethodTable extends AbstractApplicationTable {
  private static final Logger LOGGER = LoggerFactory.getLogger(SettlementMethodTable.class);
  
  protected static final String BTC_PUB_KEY_FN = "bicoinPublicKey";
  protected static final String SETTLEMENT_METHOD_TN = "settlementMethod";
  protected static final String BTC_PUB_KEY_IN = "bitcoinPKIdx";

  protected ISqlJetTable settlementMethodTable;

  public SettlementMethodTable(AbstractApplicationDB appDB) throws SqlJetException {
    super(appDB);
  }
  
  protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException {
    if(schemaNeedsToBeCreated){
      db.beginTransaction(SqlJetTransactionMode.WRITE);
      db.createTable("create table "+SETTLEMENT_METHOD_TN+"("
          + NODE_ID_FN+" BLOB PRIMARY KEY NOT NULL, "
          + BTC_PUB_KEY_FN+" BLOB NOT NULL "
          + ");");
      db.createIndex("create unique index "+BTC_PUB_KEY_IN+" on "+SETTLEMENT_METHOD_TN+"("+BTC_PUB_KEY_FN+");");
      db.commit();
    }
    settlementMethodTable=db.getTable(SETTLEMENT_METHOD_TN);
  }

  public ECKey getBitcoinSettlementMethod(final NodeIdentifier nodeId) throws SqlJetException{
    return (ECKey) db.runReadTransaction(new ISqlJetTransaction() {
      @Override public Object run(SqlJetDb db) throws SqlJetException {
        ISqlJetCursor nodeAccountCursor = settlementMethodTable.lookup(null, nodeId.getBytes());
        ECKey nodeKey=null;
        if(nodeAccountCursor.eof()==false){
          nodeKey=new ECKey(null, nodeAccountCursor.getBlobAsArray(BTC_PUB_KEY_FN));
        }
        nodeAccountCursor.close();
        return nodeKey;
      }
    });
  }

  public void setBitcointSettlementMethod(final NodeIdentifier nodeId, final ECKey remoteBitcointPublicKey) throws SqlJetException {
    ECKey existingKey=getBitcoinSettlementMethod(nodeId);
    if(existingKey!=null){
      LOGGER.error("The node {} already has the bitcoin address {} associated to it, ignoring update.", nodeId, existingKey);
      //TODO We should allow updates of the address after a few days. This is to help against sybill attacks where a node would change it's nodeId very often, but be backed by the same bitcoin address
      return;
    }

    db.runWriteTransaction(new ISqlJetTransaction() {
      @Override public Object run(SqlJetDb db) throws SqlJetException {
        byte[] publicKeyBytes=remoteBitcointPublicKey.getPubKey();
        ISqlJetCursor nodeWithExistingBTCAddressCursor = settlementMethodTable.lookup(BTC_PUB_KEY_IN, publicKeyBytes);
        boolean keyAlreadyUsedByNode=nodeWithExistingBTCAddressCursor.eof();
        nodeWithExistingBTCAddressCursor.close();
        if(keyAlreadyUsedByNode){
          LOGGER.error("The key {} is already used by node {}. Ignoring update.", remoteBitcointPublicKey, nodeId);
          return 0;
        }
        return settlementMethodTable.insert(nodeId.getBytes(), publicKeyBytes);        
      }
    });
  }
  
  
}
