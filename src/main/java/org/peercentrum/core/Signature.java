package org.peercentrum.core;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.xml.bind.DatatypeConverter;

public class Signature {
  protected byte[] signatureDERBytes;

  public Signature(byte[] encodedDERBytesOfSignature) {
    this.signatureDERBytes=encodedDERBytesOfSignature;
  }

  public static Signature createSignatureFromData(byte[] dataToSign, PrivateKey nodePrivateKey) throws Exception {
    java.security.Signature sha256Signer=java.security.Signature.getInstance("SHA256withECDSA");
    sha256Signer.initSign(nodePrivateKey);
    sha256Signer.update(dataToSign);
    return new Signature(sha256Signer.sign());
  }

  public boolean isSignatureValid(byte[] dataToVerify, PublicKey publicKey) throws Exception {
    java.security.Signature sha256Verify=java.security.Signature.getInstance("SHA256withECDSA");
    sha256Verify.initVerify(publicKey);
    sha256Verify.update(dataToVerify);
    return sha256Verify.verify(signatureDERBytes);
  }

  public byte[] getDEREncodedBytes() {
    return signatureDERBytes;
  }

  @Override
  public String toString() {
    return DatatypeConverter.printHexBinary(signatureDERBytes);
  }
  
}
