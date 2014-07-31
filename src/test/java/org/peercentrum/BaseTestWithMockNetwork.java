package org.peercentrum;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.peercentrum.blob.P2PBlobStandaloneClient;
import org.peercentrum.core.TransientMockNetworkOfNodes;

public abstract class BaseTestWithMockNetwork {
  public static TransientMockNetworkOfNodes mockNodes;
  public static P2PBlobStandaloneClient blobClient;

  @BeforeClass
  public static void setupNetwork() throws Exception{
    if(mockNodes==null){
      mockNodes=new TransientMockNetworkOfNodes();
      blobClient = new P2PBlobStandaloneClient(mockNodes.client1ToServer1Connection, mockNodes.client1Config, mockNodes.settlementClient1);
    }
  }

  @AfterClass
  public static void shutdownNetwork() throws Exception{
    if(mockNodes!=null){
      mockNodes.shutdown();
      mockNodes=null;
    }
  }

}
