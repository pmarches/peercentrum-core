package org.castaconcord.network;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.castaconcord.core.BazarroNodeDatabase;
import org.castaconcord.core.BazarroNodeIdentifier;
import org.castaconcord.core.NodeGossipApplication;
import org.junit.Test;

public class BazarroNetworkClientTest {

//	@Test
//	public void testHash2Pointer() {
//		AsyncSocketServer server = new AsyncSocketServer();
//		server.addApplicationHandler(new HashToPointerApplication());
//		
//		BazarroNetworkClient client = new BazarroNetworkClient();
//		BazarroNodeIdentifier serverId=server.getNodeIdentifier();
//		
//		HashToPointerApplication hashPointerApp=new HashToPointerApplication(client);
//		Hash256 hashKey = ...;
//		Hash256 hashValue=hashPointerApp.getPointedValue(hashKey).get();
//		hashPointerApp.updatePointedValue(hashKey, new Hash256()).sync();
////		client.sendRequestBytes(serverId, BazarroNetworkApplication.BNETWORK_APPID, applicationSpecificBytes);
//	}

//	public void testRippleApp(){
//		RippleApplication rippleApp = new RippleApplication(client);
//		rippleApp.getBalanceForAddress(address1);
//	}
	
	@Test
	public void testNodeGossip() throws Exception{
		BazarroNodeIdentifier serverId=new BazarroNodeIdentifier("ServerNode".getBytes());
		BazarroNodeDatabase serverNodeDatabase = new BazarroNodeDatabase(null);
		serverNodeDatabase.mapNodeIdToAddress(new BazarroNodeIdentifier("A new node on port 22".getBytes()), new InetSocketAddress(22));
		BazarroNetworkServer server = new BazarroNetworkServer(serverId, serverNodeDatabase, 0);
		new NodeGossipApplication(server);
		
		BazarroNodeDatabase clientNodeDatabase = new BazarroNodeDatabase(null);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
		clientNodeDatabase.mapNodeIdToAddress(serverId, serverEndpoint);
		BazarroNetworkClient client = new BazarroNetworkClient(new BazarroNodeIdentifier("ClientNode".getBytes()), clientNodeDatabase);

		//FIXME clientSideServerThatShouldNotNeedToExist
		BazarroNetworkServer clientSideServerThatShouldNotNeedToExist = new BazarroNetworkServer(client.getLocalNodeId(), clientNodeDatabase, 0);
		NodeGossipApplication clientSideGossipApp=new NodeGossipApplication(clientSideServerThatShouldNotNeedToExist);
		clientSideGossipApp.exchangeNodes(client, serverId);
		client.close();
		server.stopAcceptingConnections();
		
		assertEquals(1, serverNodeDatabase.size());
		assertEquals(2, clientNodeDatabase.size());
	}
}
