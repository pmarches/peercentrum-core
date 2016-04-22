package org.peercentrum.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;

import javax.crypto.KeyAgreement;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.Test;

public class DiffieHellmanTest {

  @Test
  public void test() throws Exception{
    KeyFactory kf=KeyFactory.getInstance("EC");

    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(256);
    KeyPair node1DHKey = keyGen.generateKeyPair();
    KeyPair node2DHKey = keyGen.generateKeyPair();
    assertFalse(node1DHKey.equals(node2DHKey));
    
    KeyAgreement ka1= KeyAgreement.getInstance("ECDH");
    ka1.init(node1DHKey.getPrivate());
    ka1.doPhase(node2DHKey.getPublic(), true);
    byte[] node1Secret = ka1.generateSecret();

    KeyAgreement ka2= KeyAgreement.getInstance("ECDH");
    ka2.init(node2DHKey.getPrivate());
    ka2.doPhase(node1DHKey.getPublic(), true);
    byte[] node2Secret = ka2.generateSecret();
    assertArrayEquals(node1Secret, node2Secret);
    System.out.println(node2Secret.length);
    
    Signature sha256Sig=Signature.getInstance("SHA256withECDSA");
    sha256Sig.initSign(node1DHKey.getPrivate());
    sha256Sig.update("Hello world!".getBytes());
    byte[] signature=sha256Sig.sign();
    System.out.println("Signature size:"+signature.length);
    
    Signature sha256Verify=Signature.getInstance("SHA256withECDSA");
    sha256Verify.initVerify(node1DHKey.getPublic());
    sha256Verify.update("Hello world!".getBytes());
    assertTrue(sha256Verify.verify(signature));
  }

  @Test
  public void listCiphersuite() throws Exception{
 // Create an SSLContext that uses our TrustManager
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, null, new SecureRandom());

    SSLParameters params = context.getSupportedSSLParameters();
    String[] suites = params.getCipherSuites();
    System.out.println("Java version : " + System.getProperty("java.runtime.version"));
    System.out.println("Connecting with " + suites.length + " cipher suites supported:");

    for (int i = 0; i < suites.length; i++) {
        System.out.println();
        System.out.print(suites[i]);
    }
  }
}
