package org.peercentrum.network;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeGossipApplication;
import org.peercentrum.core.NodeGossipClient;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;

import com.google.common.io.Files;

public class NetworkClientTest {

//	@Test
//	public void testHash2Pointer() {
//		AsyncSocketServer server1 = new AsyncSocketServer();
//		server1.addApplicationHandler(new HashToPointerApplication());
//		
//		NetworkClient client = new NetworkClient();
//		NodeIdentifier serverId=server1.getNodeIdentifier();
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
	
	@Test
	public void testNodeGossip() throws Exception{
		TopLevelConfig serverConfig=new TopLevelConfig();
    serverConfig.setBaseDirectory(Files.createTempDir());
		serverConfig.setNodeIdentifier("ServerNode");
		NetworkServer server = new NetworkServer(serverConfig);
		server.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A third node on port 2222".getBytes()), new InetSocketAddress(2222));
		new NodeGossipApplication(server);
		
    TopLevelConfig clientConfig=new TopLevelConfig();
    clientConfig.setBaseDirectory(Files.createTempDir());
    clientConfig.setNodeIdentifier("ClientNode");
		NodeDatabase clientNodeDatabase = new NodeDatabase(null);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
		clientNodeDatabase.mapNodeIdToAddress(server.getNodeIdentifier(), serverEndpoint);
		NetworkClient client = new NetworkClient(new NodeIdentity(clientConfig), clientNodeDatabase);

		NodeGossipClient clientSideGossipApp=new NodeGossipClient(client);
		clientSideGossipApp.exchangeNodes(client, server.getNodeIdentifier());
				
		client.close();
		server.stopAcceptingConnections();
		
		assertEquals(2, server.nodeDatabase.size());
		assertEquals(2, clientNodeDatabase.size());
	}
	
	 @Test
	  public void testPing() throws Exception{
	    TopLevelConfig serverConfig=new TopLevelConfig();
	    serverConfig.setBaseDirectory(Files.createTempDir());
	    serverConfig.setNodeIdentifier("ServerNode");
	    NetworkServer server = new NetworkServer(serverConfig);
	    server.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A new node on port 22".getBytes()), new InetSocketAddress(22));
	    new NodeGossipApplication(server);
	    
	    TopLevelConfig clientConfig=new TopLevelConfig();
	    clientConfig.setBaseDirectory(Files.createTempDir());
	    clientConfig.setNodeIdentifier("ClientNode");
	    NodeDatabase clientNodeDatabase = new NodeDatabase(null);
	    InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", server.getListeningPort());
	    clientNodeDatabase.mapNodeIdToAddress(server.getNodeIdentifier(), serverEndpoint);
	    NetworkClient client = new NetworkClient(new NodeIdentity(clientConfig), clientNodeDatabase);
	    
	    NetworkClientConnection connection=client.maybeOpenConnectionToPeer(server.getNodeIdentifier());
	    connection.ping();
	    connection.ping();
	    assertEquals(server.getNodeIdentifier(), connection.remoteNodeId);

      client.close();
	    server.stopAcceptingConnections();
	  }

}
