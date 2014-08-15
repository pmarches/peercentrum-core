package org.peercentrum.core;

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.peercentrum.network.NodeIdentity;

public class SignatureTest {

  @Test
  public void test() throws Exception {
    NodeIdentity nodeId1=new NodeIdentity(TopLevelConfig.loadFromFile(new File("testdata/node1/peercentrum-config.yaml")));
    System.out.println("Private "+DatatypeConverter.printHexBinary(nodeId1.getPrivateKey().getEncoded()));
    System.out.println("Public "+DatatypeConverter.printHexBinary(nodeId1.getPublicKey().getEncoded()));
    
    byte[] dataToSign="Some data to sign".getBytes();
    Signature sig1=Signature.createSignatureFromData(dataToSign, nodeId1.getPrivateKey());
    
    assertTrue(sig1.isSignatureValid(dataToSign, nodeId1.getPublicKey()));
    assertTrue(sig1.getDEREncodedBytes().length>=70);
    Signature reSerializedSig1=new Signature(sig1.getDEREncodedBytes());
    NodeIdentifier node1ReCoded=new NodeIdentifier(nodeId1.getIdentifier().getBytes());
    assertTrue(reSerializedSig1.isSignatureValid(dataToSign, node1ReCoded.getPublicKey()));
  }

}
