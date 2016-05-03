package org.peercentrum.network;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.peercentrum.core.NullConfig;
import org.peercentrum.core.ServerMain;
import org.peercentrum.nodestatistics.NodeStatisticsDatabase;

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

  //	@Test
  //	public void testNodeGossip() throws Exception{
  //		mockNodes.server1.nodeDatabase.mapNodeIdToAddress(new NodeIdentifier("A third node on port 2222".getBytes()), new InetSocketAddress(2222));
  //
  //		NodeGossipClient clientSideGossipApp=new NodeGossipClient(mockNodes.networkClient1);
  //		clientSideGossipApp.exchangeNodes(mockNodes.networkClient1, mockNodes.server1.getNodeIdentifier());
  //		
  //		assertEquals(2, mockNodes.server1.nodeDatabase.size());
  //		assertEquals(2, mockNodes.networkClient1.nodeDatabase.size());
  //	}

  @Test
  public void testConnectionFailed() throws Exception{
    NetworkClient client=new NetworkClient(new NodeIdentity(new NullConfig()), new NodeStatisticsDatabase(null));
    NodeIdentity fakeId=new NodeIdentity(new NullConfig());
    client.nodeDatabase.mapNodeIdToAddress(fakeId.getIdentifier(), InetSocketAddress.createUnresolved("192.168.1.231", 8888));
    NetworkClientConnection connection1To2 = client.createConnectionToPeer(fakeId.getIdentifier());
    Thread.sleep(1000);
    connection1To2.close();
    client.close();
  }

//  @Test
//  public void testPing() throws Exception{
//    ServerMain server1=new ServerMain(new NullConfig());
//    NetworkClient client=new NetworkClient(new NodeIdentity(new NullConfig()), new NodeStatisticsDatabase(null));
//    client.nodeDatabase.mapNodeIdToAddress(server1.getLocalIdentifier(), server1.getNetworkServer().getListeningAddress());
//    NetworkClientConnection connection1To2 = client.createConnectionToPeer(server1.getLocalIdentifier());
//    connection1To2.ping();
//    connection1To2.close();
//    server1.getNetworkServer().stopAcceptingConnections();
//    client.close();
//  }

}
