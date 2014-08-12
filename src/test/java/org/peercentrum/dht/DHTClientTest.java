package org.peercentrum.dht;

import static org.junit.Assert.*;
import io.netty.util.concurrent.DefaultPromise;

import java.util.List;

import org.junit.Test;
import org.peercentrum.core.NodeMetaData;
import org.peercentrum.core.TransientMockNetworkOfNodes;

public class DHTClientTest {

  @Test
  public void test() throws Exception {
    TransientMockNetworkOfNodes mockNetwork=new TransientMockNetworkOfNodes();
    DHTClient client=new DHTClient(mockNetwork.networkClient1);
    List<KIdentifier> twoNodes=client.getClosestNodeTo(new KIdentifier(0b001), 3);
    assertEquals(2, twoNodes.size());
//    DefaultPromise<NodeMetaData> endpoint000 = client.searchNetworkForNode(new KIdentifier("000"));
//    DefaultPromise<List<KIdentifier>> content111=client.findNodeWithContent(new KIdentifier("DEAD_BEEF"));
  }

}
