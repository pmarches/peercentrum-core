package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import io.netty.util.concurrent.Future;

import org.junit.Test;
import org.peercentrum.BaseTestWithMockNetwork;

public class P2PBlobApplicationTest extends BaseTestWithMockNetwork {

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
