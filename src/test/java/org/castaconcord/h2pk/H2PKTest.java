package org.castaconcord.h2pk;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.castaconcord.consensusprocess.ConsensusThreshold;
import org.castaconcord.consensusprocess.MockTriggerableThreshold;
import org.castaconcord.consensusprocess.UniqueNodeList;
import org.castaconcord.core.BazarroNodeDatabase;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.castaconcord.network.BazarroNetworkServer;
import org.junit.Test;


public class H2PKTest {
	@Test
	public void test() throws Exception {
		final int NB_NODES=3;
		BazarroNodeIdentifier clientNodeId=new BazarroNodeIdentifier("ClientNode");
		HashToPublicKeyStandaloneClient client=null;
		UniqueNodeList sharedUNL = new UniqueNodeList();
		BazarroNodeDatabase sharedNodeDatabase = new BazarroNodeDatabase(null);
		HashToPublicKeyApplication[] apps=new HashToPublicKeyApplication[NB_NODES]; 
		ConsensusThreshold mockThreshold=new MockTriggerableThreshold(1, NB_NODES);
		for(int i=0; i<NB_NODES; i++){
			BazarroNodeIdentifier nodeId=new BazarroNodeIdentifier("Node"+i);
			BazarroNetworkServer nodeServer = new BazarroNetworkServer(nodeId, sharedNodeDatabase, 0);
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
		BazarroHashIdentifier address=new BazarroHashIdentifier();
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
