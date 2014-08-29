package org.peercentrum.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
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
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class NodeIdentity {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeIdentity.class);
  static final String BC_PROVIDER = "BC";
  public static ECParameterSpec secp256k1 = ECNamedCurveTable.getParameterSpec("secp256k1");

  protected File localCertificateFile, localPrivateKeyFile;
  SecureRandom random;
  private NodeIdentifier localId;
  private X509Certificate cert;
  private Certificate[] localCertificateChainArray;
  private BCECPublicKey localPublicECKey;
  private BCECPrivateKey localPrivateECKey;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public NodeIdentity(TopLevelConfig config) throws Exception {
    if(config!=null){
      localCertificateFile=config.getFile("localCertificate.crt");
      localPrivateKeyFile=config.getFile("localPrivateKey.pem");
    }
    random = SecureRandom.getInstance("SHA1PRNG");
    if(localCertificateFile==null || localCertificateFile.exists()==false){
      generateKeyPair();
      generateCertificate();
      saveKeyPairAndCertificateToFile();
    }
    else{
      loadKeyPairAndCertificateFromFile();
    }
  }

  protected void loadKeyPairAndCertificateFromFile() throws Exception {
    PemReader certificatePEMReader=new PemReader(new FileReader(this.localCertificateFile));
    PemObject certificatePEM=certificatePEMReader.readPemObject();
    certificatePEMReader.close();
    CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
    this.cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificatePEM.getContent()));
    localCertificateChainArray=new Certificate[]{cert};


    PKCS8EncodedKeySpec encodedKeySpec;
    if(false){
      PemReader privateKeyReader=new PemReader(new FileReader(this.localPrivateKeyFile));
      PemObject privateKeyPEM=privateKeyReader.readPemObject();
      privateKeyReader.close();
      encodedKeySpec = new PKCS8EncodedKeySpec(privateKeyPEM.getContent());
    }
    else if(true){
      PEMParser privateKeyParser=new PEMParser(new FileReader(this.localPrivateKeyFile));
      PemObject privateKeyPEM=privateKeyParser.readPemObject();
      privateKeyParser.close();
      encodedKeySpec = new PKCS8EncodedKeySpec(privateKeyPEM.getContent());
    }
    else{
      //For whatever reason PemReader is not accessible from outside their package
      Class pemReaderClass=Class.forName("io.netty.handler.ssl.PemReader");
      Method readPrivateKeyMethod = pemReaderClass.getDeclaredMethod("readPrivateKey", File.class);
      readPrivateKeyMethod.setAccessible(true);
      ByteBuf encodedKeyBuf = (ByteBuf) readPrivateKeyMethod.invoke(null, localPrivateKeyFile);
      //    ByteBuf encodedKeyBuf = PemReader.readPrivateKey(localPrivateKeyFile);
      byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
      encodedKeyBuf.readBytes(encodedKey).release();
      encodedKeySpec = new PKCS8EncodedKeySpec(encodedKey);
    }
    KeyFactory ecKeyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
    localPrivateECKey = (BCECPrivateKey) ecKeyFactory.generatePrivate(encodedKeySpec);
    localPublicECKey = (BCECPublicKey) localCertificateChainArray[0].getPublicKey();

    localId=new NodeIdentifier(localPublicECKey);
    LOGGER.debug("Loaded identity "+localPublicECKey);
    LOGGER.debug("Loaded identity "+localId);
  }

  protected void saveKeyPairAndCertificateToFile() throws Exception {
    if(localPrivateKeyFile==null){
      LOGGER.info("not saving private key nor certificate");
      return;
    }
    //Encode in PEM format, the format prefered by openssl
    if(false){
      PEMWriter pemWriter=new PEMWriter(new FileWriter(localPrivateKeyFile));
      pemWriter.writeObject(localPrivateECKey);
      pemWriter.close();
    }
    else{
      String keyText = "-----BEGIN EC PRIVATE KEY-----\n" +
          Base64.encode(Unpooled.wrappedBuffer(localPrivateECKey.getEncoded()), true).toString(CharsetUtil.US_ASCII) +
          "\n-----END EC PRIVATE KEY-----\n";
      Files.write(keyText, localPrivateKeyFile, CharsetUtil.US_ASCII);

      Files.write(localId.toString(), new File(localPrivateKeyFile.getParentFile(), "localPublic.hash"), CharsetUtil.US_ASCII);
    }

    PEMWriter certificateWriter=new PEMWriter(new FileWriter(localCertificateFile));
    certificateWriter.writeObject(cert);
    certificateWriter.close();
    LOGGER.info("Saved to "+localCertificateFile.getAbsolutePath());
  }

  private void generateCertificate() throws Exception {
    Date NOT_BEFORE=new Date();
    Calendar NOT_AFTER=Calendar.getInstance();
    NOT_AFTER.add(Calendar.YEAR, 100);
    X500Name subjectAndIssuer= new X500Name("CN=peercentrum node");
    X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
        subjectAndIssuer, new BigInteger(64, random), NOT_BEFORE, NOT_AFTER.getTime(), subjectAndIssuer, localPublicECKey);

    ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(BC_PROVIDER).build(localPrivateECKey);
    X509CertificateHolder certHolder = certificateBuilder.build(signer);
    cert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(certHolder);

    //    if(certHolder.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(BC_PROVIDER).build(localKeypair.getPublic()))==false){
    //      throw new Exception("Verification failed");
    //    }
    cert.verify(localPublicECKey, BC_PROVIDER);
    localCertificateChainArray=new Certificate[] {cert};
  }

  public void generateKeyPair(){
    try {
      LOGGER.info("Generating new keypair");
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
      keyGen.initialize(secp256k1, random);
      KeyPair localKeypair = keyGen.generateKeyPair();
      localPrivateECKey=(BCECPrivateKey) localKeypair.getPrivate();
      localPublicECKey=(BCECPublicKey) localKeypair.getPublic();
      localId=new NodeIdentifier(localPublicECKey);
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  public NodeIdentifier getIdentifier() {
    return localId;
  }

  public PrivateKey getPrivateKey() throws Exception {
    return localPrivateECKey;
  }

  public Certificate[] getNodeCertificate() throws Exception {
    return localCertificateChainArray;
  }

  public PublicKey getPublicKey() {
    return localPublicECKey;
  }

}
