package org.peercentrum.network;

import java.io.File;

import org.junit.Test;
import org.peercentrum.core.TopLevelConfig;

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

  class NullConfig extends TopLevelConfig{
    @Override
    public File getFile(String fileName) {
      return null;
    }
  }
  
  @Test
  public void testPing() throws Exception{
    NetworkServer server1=new NetworkServer(new NullConfig());
    NetworkServer server2=new NetworkServer(new NullConfig());
    server1.nodeDatabase.mapNodeIdToAddress(server2.getNodeIdentifier(), server2.getListeningAddress());
    NetworkClientConnection connection1To2 = server1.networkClient.createConnectionToPeer(server2.getNodeIdentifier());
    connection1To2.ping();
    connection1To2.close();
  }

}
