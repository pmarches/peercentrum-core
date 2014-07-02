package org.castaconcord.network;

import org.castaconcord.core.NodeDatabase;
import org.castaconcord.core.NodeIdentifier;

public abstract class NetworkClientOrServer {
	NodeDatabase nodeDatabase;
	NodeIdentifier thisNodeId;

	public NetworkClientOrServer(NodeIdentifier thisNodeId, NodeDatabase nodeDb) {
		this.thisNodeId=thisNodeId;
		this.nodeDatabase=nodeDb;
	}
	
	public NodeDatabase getNodeDatabase(){
		return nodeDatabase;
	}
	
	public NodeIdentifier getLocalNodeId(){
		return thisNodeId;
	}
}
