package org.peercentrum.dht;

import static org.junit.Assert.*;
import io.netty.util.concurrent.DefaultPromise;

import java.util.List;

import org.junit.Test;
import org.peercentrum.PermanentMockNetwork;
import org.peercentrum.dht.selfregistration.SelfRegistrationDHT;

public class SelfRegistrationDHTTest {  
  @Test
  public void test() throws Exception {
//    TransientMockNetworkOfNodes mockNetwork=new TransientMockNetworkOfNodes();
    PermanentMockNetwork mockNetwork=new PermanentMockNetwork();
    SelfRegistrationDHT dht0=(SelfRegistrationDHT) mockNetwork.server[0].getApplicationHandler(SelfRegistrationDHT.APP_ID);
    DHTClient client0=dht0.dhtClient;
    
//    assertEquals(dht0.dhtClient.buckets.size(), mockNetwork.server[0].getNodeDatabase().size());
    
    KIdentifier server1=new KIdentifier(mockNetwork.server[1].getNodeIdentifier());
    List<KIdentifier> oneNode=client0.buckets.getClosestNodeTo(server1, 3);
    assertEquals(1, oneNode.size());
    DefaultPromise<DHTSearch> searchNode0 = client0.searchNetwork(server1);
    assertTrue(searchNode0.get().isDone());
    assertTrue(searchNode0.get().foundNode);
    assertNull(searchNode0.get().foundValue);
    
    KIdentifier server5=new KIdentifier(mockNetwork.server[5].getNodeIdentifier());
    DefaultPromise<DHTSearch> searchNode5 = client0.searchNetwork(server5);
    assertTrue(searchNode5.get().isDone());
    assertTrue(searchNode5.get().foundNode);
    assertNull(searchNode5.get().foundValue);

    KIdentifier storedKey1=new KIdentifier(PermanentMockNetwork.STORED_KEY1);
    DefaultPromise<DHTSearch> searchStoredKey1 = client0.searchNetwork(storedKey1);
    assertTrue(searchStoredKey1.get().isDone());
    assertFalse(searchStoredKey1.get().foundNode);
    assertNotNull(searchStoredKey1.get().foundValue);

//    DefaultPromise<List<KIdentifier>> content111=client.findNodeWithContent(new KIdentifier("DEAD_BEEF"));
  }

}
