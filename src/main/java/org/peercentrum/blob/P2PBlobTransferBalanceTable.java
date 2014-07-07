package org.peercentrum.blob;

import org.peercentrum.core.AbstractApplicationDB;
import org.peercentrum.core.AbstractNodeBalanceTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.sqljet.core.SqlJetException;

public class P2PBlobTransferBalanceTable extends AbstractNodeBalanceTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(P2PBlobTransferBalanceTable.class);

	public P2PBlobTransferBalanceTable(AbstractApplicationDB appDB) throws SqlJetException {
		super(appDB, "blobAccounting");
	}
	
}
