package org.peercentrum.network;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.BaseTestWithMockNetwork;
import org.peercentrum.core.NodeGossipApplication;
import org.peercentrum.core.NodeGossipClient;
import org.peercentrum.core.NodeIdentifier;

public class NetworkClientTest extends BaseTestWithMockNetwork {

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
		mockNodes.server1.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A third node on port 2222".getBytes()), new InetSocketAddress(2222));

		NodeGossipClient clientSideGossipApp=new NodeGossipClient(mockNodes.networkClient1);
		clientSideGossipApp.exchangeNodes(mockNodes.networkClient1, mockNodes.server1.getNodeIdentifier());
		
		assertEquals(2, mockNodes.server1.nodeDatabase.size());
		assertEquals(2, mockNodes.networkClient1.nodeDatabase.size());
	}
	
	 @Test
	  public void testPing() throws Exception{
	    new NodeGossipApplication(mockNodes.server1);
	    mockNodes.client1ToServer1Connection.ping();
	    mockNodes.client1ToServer1Connection.ping();
	    assertEquals(mockNodes.server1.getNodeIdentifier(), mockNodes.client1ToServer1Connection.remoteNodeId);
	  }

}
