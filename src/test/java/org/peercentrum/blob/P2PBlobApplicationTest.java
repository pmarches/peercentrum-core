package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.netty.util.concurrent.Future;

import org.junit.Test;
import org.peercentrum.core.TransientMockNetworkOfNodes;

public class P2PBlobApplicationTest {

	@Test
	public void test() throws Exception {
	  TransientMockNetworkOfNodes mockNodes=new TransientMockNetworkOfNodes();

	  try{
	    P2PBlobStandaloneClient blobClient = new P2PBlobStandaloneClient(mockNodes.clientToServerConnection, 
	        mockNodes.client1Config, mockNodes.settlementClient1);
	    P2PBlobStoredBlobMemoryOnly hellowWorldDownload=new P2PBlobStoredBlobMemoryOnly(mockNodes.helloWorldBlobID);
	    Future<P2PBlobStoredBlob> hellowWorldDownloadCompleteFuture=blobClient.downloadAll(hellowWorldDownload);
	    hellowWorldDownloadCompleteFuture.sync();
	    assertNotNull(hellowWorldDownload.getHashList());
	    assertEquals(12, hellowWorldDownload.downloadedAndValidatedBlobContent.readableBytes());
	    assertEquals(12, hellowWorldDownload.blobLengthInBytes);
	  }
	  finally{
      mockNodes.shutdown();
	  }
	}

}
