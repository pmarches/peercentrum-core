package org.peercentrum.h2pk;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.consensusprocess.ConsensusThreshold;
import org.peercentrum.consensusprocess.MockTriggerableThreshold;
import org.peercentrum.consensusprocess.UniqueNodeList;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkServer;


public class H2PKTest {
	@Test
	public void test() throws Exception {
		final int NB_NODES=3;
		NodeIdentifier clientNodeId=new NodeIdentifier("ClientNode");
		HashToPublicKeyStandaloneClient client=null;
		UniqueNodeList sharedUNL = new UniqueNodeList();
		NodeDatabase sharedNodeDatabase = new NodeDatabase(null);
		HashToPublicKeyApplication[] apps=new HashToPublicKeyApplication[NB_NODES]; 
		ConsensusThreshold mockThreshold=new MockTriggerableThreshold(1, NB_NODES);
		for(int i=0; i<NB_NODES; i++){
			TopLevelConfig topConfig=new TopLevelConfig("Node"+i);
			NetworkServer nodeServer = new NetworkServer(topConfig);
			InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", nodeServer.getListeningPort());
			sharedNodeDatabase.mapNodeIdToAddress(nodeServer.getLocalNodeId(), serverEndpoint);
			sharedUNL.addValidatorNode(nodeServer.getLocalNodeId());
			HashToPublicKeyDB db=new HashToPublicKeyDB();
			apps[i]=new HashToPublicKeyApplication(nodeServer, db, sharedUNL);
//			apps[i].consensus.consensusThreshold=mockThreshold;
			if(client==null){
				client=new HashToPublicKeyStandaloneClient(nodeServer.getLocalNodeId(), clientNodeId, sharedNodeDatabase);
			}
		}
		HashIdentifier address=new HashIdentifier();
		client.registerForAddress(address, clientNodeId);
		client.close();
		for(HashToPublicKeyApplication app : apps){
			app.startDBCloseProcess(0);
		}
		apps[0].db.awaitForVersion(3);
		assertEquals(3, apps[0].db.getDBVersionNumber().intValue());
		assertEquals(1, apps[0].db.getRegisteredPublicKeysForAddress(address).size());
	}

}
