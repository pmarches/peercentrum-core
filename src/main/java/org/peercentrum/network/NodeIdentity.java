package org.peercentrum.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import org.bouncycastle.openssl.PEMWriter;
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
  static ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

  protected File localCertificateFile, localPrivateKeyFile;
  KeyPair localKeypair;
  SecureRandom random;
  private NodeIdentifier localId;
  private X509Certificate cert;
  private Certificate[] localCertificateChainArray;

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
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    this.cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certificatePEM.getContent()));
    localCertificateChainArray=new Certificate[]{cert};


    PemReader privateKeyReader=new PemReader(new FileReader(this.localPrivateKeyFile));
    PemObject privateKeyPEM=privateKeyReader.readPemObject();
    privateKeyReader.close();
    PKCS8EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(privateKeyPEM.getContent());
    KeyFactory ecKeyFactory = KeyFactory.getInstance("EC", BC_PROVIDER);
    PrivateKey localPrivateECKey = ecKeyFactory.generatePrivate(encodedKeySpec);
    PublicKey localPublicECKey = localCertificateChainArray[0].getPublicKey();
    localKeypair=new KeyPair(localPublicECKey, localPrivateECKey);
    LOGGER.debug("Loaded identity "+localKeypair.getPublic());
  }

  protected void saveKeyPairAndCertificateToFile() throws Exception {
    //Encode in PEM format, the format prefered by openssl
    PEMWriter pemWriter=new PEMWriter(new FileWriter(localPrivateKeyFile));
    pemWriter.writeObject(localKeypair.getPrivate());
    pemWriter.close();

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
        subjectAndIssuer, new BigInteger(64, random), NOT_BEFORE, NOT_AFTER.getTime(), subjectAndIssuer, localKeypair.getPublic());

    ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").setProvider(BC_PROVIDER).build(localKeypair.getPrivate());
    X509CertificateHolder certHolder = certificateBuilder.build(signer);
    cert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(certHolder);

    //    if(certHolder.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(BC_PROVIDER).build(localKeypair.getPublic()))==false){
    //      throw new Exception("Verification failed");
    //    }
    cert.verify(localKeypair.getPublic(), BC_PROVIDER);
    localCertificateChainArray=new Certificate[] {cert};
  }

  public void generateKeyPair(){
    try {
      LOGGER.info("Generating new keypair");
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
