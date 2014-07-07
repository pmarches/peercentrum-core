package org.peercentrum.settlement;

import org.peercentrum.core.AbstractApplicationDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;

import com.google.bitcoin.core.NetworkParameters;

public class SettlementDB extends AbstractApplicationDB {
  private static final Logger LOGGER = LoggerFactory.getLogger(SettlementDB.class);
  public SettlementMethodTable settlementMethod;
  
  public SettlementDB(NetworkParameters params) throws SqlJetException {
    super("settlementDb.dat");
    settlementMethod=new SettlementMethodTable(params, this);
  }
}
