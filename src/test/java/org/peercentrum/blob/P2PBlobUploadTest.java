package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class P2PBlobUploadTest {
  @Test
  public void testCreateFromFile() throws Exception {
    P2PBlobUpload bigFileUpload=new P2PBlobUpload(new File("/tmp/bigFile"));
    assertEquals("51FA8E5685B6E586B7D0E45EDC1079457D828DA9CAD64EAE8AF9F742E057D694", bigFileUpload.getHashList().getTopLevelHash().toString());
    bigFileUpload.close();
    
    P2PBlobUpload oneBlock=new P2PBlobUpload(new File("/tmp/oneBlock"));
    assertEquals("C67554C8836DD666772CA9EECCC27BDE97704632FD4CA9BB898D775216CC18CF", oneBlock.getHashList().getTopLevelHash().toString());
    oneBlock.close();
  }
  
  @Test
  public void testInMemory() throws Exception{
    P2PBlobUpload bonjourMondeUpload=new P2PBlobUpload("Bonjour monde!\n".getBytes());
    assertEquals("3EC129755B093D2B403C893D33322D933D7F2C0889F70FBA75662D8319FF08A6", bonjourMondeUpload.getHashList().getTopLevelHash().toString());
    bonjourMondeUpload.close();
    
    P2PBlobUpload oneBlockMemory=new P2PBlobUpload(new byte[P2PBlobApplication.BLOCK_SIZE]);
    assertEquals("C67554C8836DD666772CA9EECCC27BDE97704632FD4CA9BB898D775216CC18CF", oneBlockMemory.getHashList().getTopLevelHash().toString());
    oneBlockMemory.close();
    
    P2PBlobUpload twoBlockMemory=new P2PBlobUpload(new byte[2*P2PBlobApplication.BLOCK_SIZE]);
    assertEquals("0CC19DD4AC4CFB8E93499A43E86E4310545F0CF73E957823834BE190CDA7835E", twoBlockMemory.getHashList().getTopLevelHash().toString());
    twoBlockMemory.close();
}

}
