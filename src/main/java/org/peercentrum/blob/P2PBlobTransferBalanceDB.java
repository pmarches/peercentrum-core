package org.peercentrum.blob;

import org.peercentrum.core.AbstractNodeBalanceDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;

public class P2PBlobTransferBalanceDB extends AbstractNodeBalanceDB {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobTransferBalanceDB.class);

	public P2PBlobTransferBalanceDB(String blobAccountingDbPath) throws SqlJetException {
		super(blobAccountingDbPath, "blobAccounting");
	}
	
}
