package org.castaconcord.h2pk;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.castaconcord.consensusprocess.ConsensusThreshold;
import org.castaconcord.consensusprocess.MockTriggerableThreshold;
import org.castaconcord.consensusprocess.UniqueNodeList;
import org.castaconcord.core.NodeDatabase;
import org.castaconcord.core.NodeIdentifier;
import org.castaconcord.network.NetworkServer;
import org.junit.Test;


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
			NodeIdentifier nodeId=new NodeIdentifier("Node"+i);
			NetworkServer nodeServer = new NetworkServer(nodeId, sharedNodeDatabase, 0);
			InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", nodeServer.getListeningPort());
			sharedNodeDatabase.mapNodeIdToAddress(nodeId, serverEndpoint);
			sharedUNL.addValidatorNode(nodeId);
			HashToPublicKeyDB db=new HashToPublicKeyDB();
			apps[i]=new HashToPublicKeyApplication(nodeServer, db, sharedUNL);
//			apps[i].consensus.consensusThreshold=mockThreshold;
			if(client==null){
				client=new HashToPublicKeyStandaloneClient(nodeId, clientNodeId, sharedNodeDatabase);
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
