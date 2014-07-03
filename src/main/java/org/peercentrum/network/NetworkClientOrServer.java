package org.peercentrum.network;

import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;

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
