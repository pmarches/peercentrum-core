package org.castaconcord.network;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.castaconcord.core.NodeDatabase;
import org.castaconcord.core.NodeIdentifier;
import org.castaconcord.core.NodeGossipApplication;
import org.junit.Test;

public class NetworkClientTest {

//	@Test
//	public void testHash2Pointer() {
//		AsyncSocketServer server = new AsyncSocketServer();
//		server.addApplicationHandler(new HashToPointerApplication());
//		
//		NetworkClient client = new NetworkClient();
//		NodeIdentifier serverId=server.getNodeIdentifier();
//		
//		HashToPointerApplication hashPointerApp=new HashToPointerApplication(client);
//		Hash256 hashKey = ...;
//		Hash256 hashValue=hashPointerApp.getPointedValue(hashKey).get();
//		hashPointerApp.updatePointedValue(hashKey, new Hash256()).sync();
////		client.sendRequestBytes(serverId, NetworkApplication.BNETWORK_APPID, applicationSpecificBytes);
//	}

//	public void testRippleApp(){
//		RippleApplication rippleApp = new RippleApplication(client);
//		rippleApp.getBalanceForAddress(address1);
//	}
	
	@Test
	public void testNodeGossip() throws Exception{
		NodeIdentifier serverId=new NodeIdentifier("ServerNode".getBytes());
		NodeDatabase serverNodeDatabase = new NodeDatabase(null);
		serverNodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A new node on port 22".getBytes()), new InetSocketAddress(22));
		NetworkServer server = new NetworkServer(serverId, serverNodeDatabase, 0);
		new NodeGossipApplication(server);
		
		NodeDatabase clientNodeDatabase = new NodeDatabase(null);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
		clientNodeDatabase.mapNodeIdToAddress(serverId, serverEndpoint);
		NetworkClient client = new NetworkClient(new NodeIdentifier("ClientNode".getBytes()), clientNodeDatabase);

		//FIXME clientSideServerThatShouldNotNeedToExist
		NetworkServer clientSideServerThatShouldNotNeedToExist = new NetworkServer(client.getLocalNodeId(), clientNodeDatabase, 0);
		NodeGossipApplication clientSideGossipApp=new NodeGossipApplication(clientSideServerThatShouldNotNeedToExist);
		clientSideGossipApp.exchangeNodes(client, serverId);
		client.close();
		server.stopAcceptingConnections();
		
		assertEquals(1, serverNodeDatabase.size());
		assertEquals(2, clientNodeDatabase.size());
	}
}
