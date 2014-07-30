package org.peercentrum.network;

import io.netty.handler.ssl.SslHandler;

import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;


public class ECDSASslContext {
  private static final String[] TLS_VERSIONS = new String[]{"TLSv1.2"};
  protected SSLContext sslContext;
  protected SSLEngine sslEngine;

  public static final String[] SUPPORTED_CIPHER_SUITE = new String[]{
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    //      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    //      "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
  };

  public ECDSASslContext(NodeIdentity nodeIdentity, TrustManager[] trustManager, boolean isClient) throws Exception {
    long sessionCacheSize=0;
    long sessionTimeout=0;

    try {
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(null, null);
      String keyStorePassword="";
      keyStore.setKeyEntry("key", nodeIdentity.getNodePrivateKey(), keyStorePassword.toCharArray(), nodeIdentity.getNodeCertificate());

      // Set up key manager factory to use our key store
      String keyManagementAlgorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
      if (keyManagementAlgorithm == null) {
        keyManagementAlgorithm = "SunX509";
      }
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyManagementAlgorithm);
      kmf.init(keyStore, keyStorePassword.toCharArray());

      // Initialize the SSLContext to work with our key managers.
      sslContext=SSLContext.getInstance(TLS_VERSIONS[0]);
      sslContext.init(kmf.getKeyManagers(), trustManager, null);
      sslEngine = sslContext.createSSLEngine();
      sslEngine.setUseClientMode(isClient);
      sslEngine.setNeedClientAuth(true);
      sslEngine.setEnabledCipherSuites(SUPPORTED_CIPHER_SUITE);
      sslEngine.setEnabledProtocols(TLS_VERSIONS);

      SSLSessionContext sessCtx = sslContext.getServerSessionContext();
      if (sessionCacheSize > 0) {
        sessCtx.setSessionCacheSize((int) Math.min(sessionCacheSize, Integer.MAX_VALUE));
      }
      if (sessionTimeout > 0) {
        sessCtx.setSessionTimeout((int) Math.min(sessionTimeout, Integer.MAX_VALUE));
      }
    } catch (Exception e) {
      throw new SSLException("failed to initialize the server-side SSL context", e);
    }

  }

  public SslHandler newHandler() {
    return new SslHandler(sslEngine);
  }
}
