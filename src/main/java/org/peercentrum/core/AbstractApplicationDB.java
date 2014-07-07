package org.peercentrum.core;

import java.io.File;

import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.table.SqlJetDb;

public class AbstractApplicationDB  implements AutoCloseable {
  protected SqlJetDb db;
  boolean schemaNeedsToBeCreated;

  public AbstractApplicationDB(String dbPath) throws SqlJetException {
    synchronized(this){
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
    }
  }
  

  @Override
  public void close() throws Exception {
    db.close();
  }

}
