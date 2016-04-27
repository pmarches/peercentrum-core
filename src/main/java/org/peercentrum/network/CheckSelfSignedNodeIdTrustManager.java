package org.peercentrum.network;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.peercentrum.core.NodeIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.EmptyArrays;

public class CheckSelfSignedNodeIdTrustManager implements X509TrustManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSelfSignedNodeIdTrustManager.class);
  
  private NodeIdentifier expectedNodeId;

  public CheckSelfSignedNodeIdTrustManager(NodeIdentifier acceptedNodeId) {
    this.expectedNodeId=acceptedNodeId;
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    LOGGER.debug("Check Client trusted "+Arrays.asList(chain)+" "+authType);
    verifyCertificate(chain);
  }

  private void verifyCertificate(X509Certificate[] chain) throws CertificateException {
    if(chain.length!=1){
      throw new CertificateException("Was expecting a single self-signed certificate, got "+chain.length+" certificates instead");
    }
    try {
      chain[0].checkValidity();
      chain[0].verify(chain[0].getPublicKey(), "BC"); //Ensure the certificate has been self-signed

      if(expectedNodeId!=null){ //expectedNodeId will be null on the networkServer side..
        BCECPublicKey publicKeyOnCertificate=new BCECPublicKey((ECPublicKey) chain[0].getPublicKey(), null);
        NodeIdentifier nodeIdOnCertificate=new NodeIdentifier(publicKeyOnCertificate);
        if(expectedNodeId.equals(nodeIdOnCertificate)==false){
          throw new CertificateException("The certificate is valid for node "+nodeIdOnCertificate+", but we were expecting to connect to node "+expectedNodeId);
        }
      }
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException e) {
      throw new CertificateException(e);
    }
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
    LOGGER.debug("Client will check if networkServer can be trusted "+Arrays.asList(chain)+" "+authType);
    if(expectedNodeId==null){
      throw new CertificateException("The client needs to know in advance what is the expected node id");
    }
    verifyCertificate(chain);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return EmptyArrays.EMPTY_X509_CERTIFICATES;
  }
}