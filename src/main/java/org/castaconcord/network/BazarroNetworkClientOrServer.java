package org.castaconcord.network;

import org.castaconcord.core.BazarroNodeDatabase;
import org.castaconcord.core.BazarroNodeIdentifier;

public abstract class BazarroNetworkClientOrServer {
	BazarroNodeDatabase nodeDatabase;
	BazarroNodeIdentifier thisNodeId;

	public BazarroNetworkClientOrServer(BazarroNodeIdentifier thisNodeId, BazarroNodeDatabase nodeDb) {
		this.thisNodeId=thisNodeId;
		this.nodeDatabase=nodeDb;
	}
	
	public BazarroNodeDatabase getNodeDatabase(){
		return nodeDatabase;
	}
	
	public BazarroNodeIdentifier getLocalNodeId(){
		return thisNodeId;
	}
}
