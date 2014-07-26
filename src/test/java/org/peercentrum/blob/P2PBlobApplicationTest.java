package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import io.netty.util.concurrent.Future;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.peercentrum.core.TransientMockNetworkOfNodes;

public class P2PBlobApplicationTest {
  static TransientMockNetworkOfNodes mockNodes;
  static P2PBlobStandaloneClient blobClient;

  @BeforeClass
  public static void setupNetwork() throws Exception{
    mockNodes=new TransientMockNetworkOfNodes();
    blobClient = new P2PBlobStandaloneClient(mockNodes.clientToServerConnection, mockNodes.client1Config, mockNodes.settlementClient1);
  }

  @AfterClass
  public static void shutdownNetwork() throws Exception{
    mockNodes.shutdown();
  }

  @Test
  public void testDownload() throws Exception {
    P2PBlobStoredBlobMemoryOnly hellowWorldDownload=new P2PBlobStoredBlobMemoryOnly(mockNodes.helloWorldBlobID);
    Future<P2PBlobStoredBlob> hellowWorldDownloadCompleteFuture=blobClient.downloadAll(hellowWorldDownload);
    hellowWorldDownloadCompleteFuture.sync();
    assertEquals(mockNodes.helloWorldBlobID, hellowWorldDownload.getHashList().getTopLevelHash());
    assertEquals(12, hellowWorldDownload.validatedBlobContent.readableBytes());
    assertEquals(12, hellowWorldDownload.getBlockLayout().getLengthOfBlob());
  }

  @Test
  public void testUpload() throws Exception {
    P2PBlobStoredBlob bonjourMondeUpload=new P2PBlobStoredBlobMemoryOnly("Bonjour monde!\n".getBytes());
    assertEquals("3EC129755B093D2B403C893D33322D933D7F2C0889F70FBA75662D8319FF08A6", bonjourMondeUpload.getHashList().getTopLevelHash().toString());
    blobClient.upload(bonjourMondeUpload);
    P2PBlobStoredBlob bonjourMondeDownload=new P2PBlobStoredBlobMemoryOnly(bonjourMondeUpload.getHashList().getTopLevelHash());
    Future<P2PBlobStoredBlob> downloadFuture = blobClient.downloadAll(bonjourMondeDownload);
    assertEquals(bonjourMondeUpload, downloadFuture.get());
  }
}
