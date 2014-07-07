package org.peercentrum.core;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

abstract public class AbstractApplicationTable {
  protected static final String NODE_ID_FN = "nodeId";
  protected SqlJetDb db;

  public AbstractApplicationTable(AbstractApplicationDB appDB) throws SqlJetException {
    this.db=appDB.db;
    if(appDB.schemaNeedsToBeCreated){
      maybeCreateSchema(appDB.schemaNeedsToBeCreated);
    }
  }


  abstract protected void maybeCreateSchema(boolean schemaNeedsToBeCreated) throws SqlJetException;
}
