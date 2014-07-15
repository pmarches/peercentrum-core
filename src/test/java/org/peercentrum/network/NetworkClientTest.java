package org.peercentrum.network;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeGossipApplication;
import org.peercentrum.core.NodeGossipClient;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;

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
////		client.sendRequestBytes(serverId, NetworkApplication.NETWORK_APPID, applicationSpecificBytes);
//	}

//	public void testRippleApp(){
//		RippleApplication rippleApp = new RippleApplication(client);
//		rippleApp.getBalanceForAddress(address1);
//	}
	
//	@Test
	public void testNodeGossip() throws Exception{
		TopLevelConfig serverConfig=new TopLevelConfig();
		serverConfig.setNodeIdentifier("ServerNode");
		NetworkServer server = new NetworkServer(serverConfig);
		server.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A new node on port 22".getBytes()), new InetSocketAddress(22));
		new NodeGossipApplication(server);
		
		NodeDatabase clientNodeDatabase = new NodeDatabase(null);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
		clientNodeDatabase.mapNodeIdToAddress(server.getLocalNodeId(), serverEndpoint);
		NetworkClient client = new NetworkClient(new NodeIdentifier("ClientNode".getBytes()), clientNodeDatabase);

		NodeGossipClient clientSideGossipApp=new NodeGossipClient(client.getLocalNodeId(), client.getNodeDatabase());
		clientSideGossipApp.exchangeNodes(client, server.getLocalNodeId());
				
		client.close();
		server.stopAcceptingConnections();
		
		assertEquals(1, server.nodeDatabase.size());
		assertEquals(2, clientNodeDatabase.size());
	}
	
	 @Test
	  public void testPing() throws Exception{
	    TopLevelConfig serverConfig=new TopLevelConfig();
	    serverConfig.setNodeIdentifier("ServerNode");
	    NetworkServer server = new NetworkServer(serverConfig);
	    server.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A new node on port 22".getBytes()), new InetSocketAddress(22));
	    new NodeGossipApplication(server);
	    
	    NodeDatabase clientNodeDatabase = new NodeDatabase(null);
	    InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
	    clientNodeDatabase.mapNodeIdToAddress(server.getLocalNodeId(), serverEndpoint);
	    NetworkClient client = new NetworkClient(new NodeIdentifier("ClientNode".getBytes()), clientNodeDatabase);
	    
	    System.out.println("server port "+server.getListeningPort());
	    client.ping(server.getLocalNodeId());
      client.ping(server.getLocalNodeId());

      client.close();
	    server.stopAcceptingConnections();
	  }

}
