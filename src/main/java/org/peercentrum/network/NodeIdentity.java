package org.peercentrum.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeIdentity {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeIdentity.class);
  
  static final String BC_PROVIDER = "BC";
  protected File localCertificateFile, localPrivateKeyFile;
  KeyPair localKeypair;
  SecureRandom random;
  X509Certificate cert;
  private NodeIdentifier localId;
  private Certificate[] localCertificateChainArray;
  static ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public NodeIdentity(TopLevelConfig config) throws Exception {
    localCertificateFile=config.getFile("localCertificate.crt");
    localPrivateKeyFile=config.getFile("localPrivateKey.pem");
    random = SecureRandom.getInstance("SHA1PRNG");
    if(localCertificateFile.exists()==false){
      generateKeyPair();
      generateCertificate();
      saveKeyPairAndCertificateToFile();
    }
    else{
      loadKeyPairAndCertificateFromFile();
    }
    
    localId=new NodeIdentifier(localKeypair.getPublic().getEncoded());
  }

  protected void loadKeyPairAndCertificateFromFile() throws Exception {
    PemReader certificatePEMReader=new PemReader(new FileReader(this.localCertificateFile));
    PemObject certificatePEM=certificatePEMReader.readPemObject();
    certificatePEMReader.close();
//    //For whatever reason PemReader is not accessible from outside their package
//    Class pemReaderClass=Class.forName("io.netty.handler.ssl.PemReader");
//    Method readCertificateMethod = pemReaderClass.getDeclaredMethod("readCertificates", File.class);
//    readCertificateMethod.setAccessible(true);
//    ByteBuf[] certs = (ByteBuf[]) readCertificateMethod.invoke(null, localCertificateFile);

    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    localCertificateChainArray=new Certificate[]{cf.generateCertificate(new ByteArrayInputStream(certificatePEM.getContent()))};
//    List<Certificate> localCertificateChain = new ArrayList<Certificate>();
    try {
//      for (ByteBuf buf: certs) {
//        localCertificateChain.add(cf.generateCertificate(new ByteBufInputStream(buf)));
//      }
    } finally {
//      for (ByteBuf buf: certs) {
//        buf.release();
//      }
    }
//    localCertificateChainArray=localCertificateChain.toArray(new Certificate[localCertificateChain.size()]);


    PemReader reader=new PemReader(new FileReader(this.localPrivateKeyFile));
    PemObject privateKeyPEM=reader.readPemObject();
    reader.close();

//    //For whatever reason PemReader is not accessible from outside their package
//    Method readPrivateKeyMethod = pemReaderClass.getDeclaredMethod("readPrivateKey", File.class);
//    readPrivateKeyMethod.setAccessible(true);
//    ByteBuf encodedKeyBuf = (ByteBuf) readPrivateKeyMethod.invoke(null, localPrivateKeyFile);
//    //    ByteBuf encodedKeyBuf = PemReader.readPrivateKey(localPrivateKeyFile);
//    byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
//    encodedKeyBuf.readBytes(encodedKey).release();
    
    PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(privateKeyPEM.getContent());
    KeyFactory ecKeyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
    PrivateKey localPrivateECKey = ecKeyFactory.generatePrivate(encodedKeySpec);
    PublicKey localPublicECKey = localCertificateChainArray[0].getPublicKey();
    localKeypair=new KeyPair(localPublicECKey, localPrivateECKey);
    LOGGER.debug("Loaded identity "+localKeypair.getPublic());
  }

  protected void saveKeyPairAndCertificateToFile() throws Exception {
    String keyText = "-----BEGIN PRIVATE KEY-----\n" +
        Base64.encode(Unpooled.wrappedBuffer(localKeypair.getPrivate().getEncoded()), true).toString(CharsetUtil.US_ASCII) +
        "\n-----END PRIVATE KEY-----\n";

    OutputStream keyOutStream = new FileOutputStream(localPrivateKeyFile);
    try {
      keyOutStream.write(keyText.getBytes(CharsetUtil.US_ASCII));
    } finally {
      keyOutStream.close();
    }

    // Encode the certificate into a CRT file.
    String certText = "-----BEGIN CERTIFICATE-----\n" +
        Base64.encode(Unpooled.wrappedBuffer(cert.getEncoded()), true).toString(CharsetUtil.US_ASCII) +
        "\n-----END CERTIFICATE-----\n";

    OutputStream certOut = new FileOutputStream(localCertificateFile);
    try {
      certOut.write(certText.getBytes(CharsetUtil.US_ASCII));
    } finally {
      certOut.close();
    }
    System.err.println("Saved to "+localCertificateFile.getAbsolutePath());
  }

  private void generateCertificate() throws Exception {
    Date NOT_BEFORE=new Date();
    Calendar NOT_AFTER=Calendar.getInstance();
    NOT_AFTER.add(Calendar.YEAR, 100);
    X500Name subjectAndIssuer= new X500Name("CN=peercentrum node");
    X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
        subjectAndIssuer, new BigInteger(64, random), NOT_BEFORE, NOT_AFTER.getTime(), subjectAndIssuer, localKeypair.getPublic());

    ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(BC_PROVIDER).build(localKeypair.getPrivate());
    X509CertificateHolder certHolder = certificateBuilder.build(signer);
    cert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(certHolder);

    //    if(certHolder.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(BC_PROVIDER).build(localKeypair.getPublic()))==false){
    //      throw new Exception("Verification failed");
    //    }
    cert.verify(localKeypair.getPublic(), BC_PROVIDER);
  }

  public void generateKeyPair(){
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
      keyGen.initialize(ecSpec, random);
      localKeypair = keyGen.generateKeyPair();
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  public NodeIdentifier getIdentifier() {
    return localId;
  }

  public PrivateKey getNodePrivateKey() throws Exception {
    return localKeypair.getPrivate();
  }

  public Certificate[] getNodeCertificate() throws Exception {
    return localCertificateChainArray;
  }

}
