package org.castaconcord.blob;

import org.castaconcord.core.AbstractNodeBalanceDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;

public class P2PBlobTransferBalanceDB extends AbstractNodeBalanceDB {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobTransferBalanceDB.class);

	public P2PBlobTransferBalanceDB(String blobAccountingDbPath) throws SqlJetException {
		super(blobAccountingDbPath, "blobAccounting");
	}
	
}
