package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class P2PBlobHashListTest {
  @Test
  public void testCreateFromFile() throws Exception {
    P2PBlobStoredBlob bigFileUpload=new P2PBlobStoredBlobRepositoryFS(new File("/tmp/bigFile"));
    assertEquals("51FA8E5685B6E586B7D0E45EDC1079457D828DA9CAD64EAE8AF9F742E057D694", bigFileUpload.getHashList().getTopLevelHash().toString());
    
    P2PBlobStoredBlob oneBlock=new P2PBlobStoredBlobRepositoryFS(new File("/tmp/oneBlock"));
    assertEquals("C67554C8836DD666772CA9EECCC27BDE97704632FD4CA9BB898D775216CC18CF", oneBlock.getHashList().getTopLevelHash().toString());
  }
  
  @Test
  public void testInMemory() throws Exception{
    P2PBlobStoredBlob bonjourMondeUpload=new P2PBlobStoredBlobMemoryOnly("Bonjour monde!\n".getBytes());
    assertEquals("3EC129755B093D2B403C893D33322D933D7F2C0889F70FBA75662D8319FF08A6", bonjourMondeUpload.getHashList().getTopLevelHash().toString());
    
    P2PBlobStoredBlob oneBlockMemory=new P2PBlobStoredBlobMemoryOnly(new byte[P2PBlobApplication.BLOCK_SIZE]);
    assertEquals("C67554C8836DD666772CA9EECCC27BDE97704632FD4CA9BB898D775216CC18CF", oneBlockMemory.getHashList().getTopLevelHash().toString());
    
    P2PBlobStoredBlob twoBlockMemory=new P2PBlobStoredBlobMemoryOnly(new byte[2*P2PBlobApplication.BLOCK_SIZE]);
    assertEquals("0CC19DD4AC4CFB8E93499A43E86E4310545F0CF73E957823834BE190CDA7835E", twoBlockMemory.getHashList().getTopLevelHash().toString());
}

}
