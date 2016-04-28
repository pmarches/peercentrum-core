package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.peercentrum.TransientMockNetworkOfNodes;
import org.peercentrum.h2pk.HashIdentifier;

import io.netty.util.concurrent.Future;

public class P2PBlobApplicationTest {
  TransientMockNetworkOfNodes mockNodes=new TransientMockNetworkOfNodes();

  @Test
  public void testDownload() throws Exception {
    P2PBlobStandaloneClient blobClient = new P2PBlobStandaloneClient(mockNodes.client1ToServer1Connection, mockNodes.client1Config, null);

    P2PBlobStoredBlobMemoryOnly bonjourMondeUpload=new P2PBlobStoredBlobMemoryOnly("Bonjour monde!\n".getBytes());
    HashIdentifier bonjourMondeHash=new HashIdentifier("3EC129755B093D2B403C893D33322D933D7F2C0889F70FBA75662D8319FF08A6");
    assertEquals(bonjourMondeHash, bonjourMondeUpload.getHashList().getTopLevelHash());
    blobClient.upload(bonjourMondeUpload);
    
    P2PBlobStoredBlobMemoryOnly bonjourMondeDownload=new P2PBlobStoredBlobMemoryOnly(bonjourMondeUpload.getHashList().getTopLevelHash());
    Future<P2PBlobStoredBlob> downloadFuture = blobClient.downloadAll(bonjourMondeDownload);
    assertEquals(bonjourMondeUpload, downloadFuture.get());

    assertEquals(bonjourMondeHash, bonjourMondeDownload.getHashList().getTopLevelHash());
    assertEquals(15, bonjourMondeDownload.validatedBlobContent.readableBytes());
    assertEquals(15, bonjourMondeDownload.getBlockLayout().getLengthOfBlob());
  }

}
