package org.peercentrum.network;

import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import io.netty.handler.ssl.SslHandler;


public class ECDSASslContext {
  private static final String[] TLS_VERSIONS = {"TLSv1.2"};
  private KeyManager[] keyManagers;
  private TrustManager[] trustManagers;

  public static final String[] ENCRYPTED_CIPHER_SUITE = {
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    //      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    //      "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
  };

  public static final String[] NON_ENCRYPTED_CIPHER_SUITE = {
    "TLS_ECDHE_ECDSA_WITH_NULL_SHA", //For DEBUG purposes only
  };

  public ECDSASslContext(NodeIdentity nodeIdentity, TrustManager trustManager) throws Exception {
    try {
      trustManagers=new TrustManager[]{trustManager};
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(null, null);
      String keyStorePassword="";
      keyStore.setKeyEntry("key", nodeIdentity.getPrivateKey(), keyStorePassword.toCharArray(), nodeIdentity.getNodeCertificate());

      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(keyStore, keyStorePassword.toCharArray());
      keyManagers=kmf.getKeyManagers();


//      long sessionCacheSize=0;
//      long sessionTimeout=0;
//      SSLSessionContext sessCtx = sslContext.getServerSessionContext();
//      if (sessionCacheSize > 0) {
//        sessCtx.setSessionCacheSize((int) Math.min(sessionCacheSize, Integer.MAX_VALUE));
//      }
//      if (sessionTimeout > 0) {
//        sessCtx.setSessionTimeout((int) Math.min(sessionTimeout, Integer.MAX_VALUE));
//      }
    } catch (Exception e) {
      throw new SSLException("failed to initialize the networkServer-side SSL context", e);
    }
  }

  public SslHandler newHandler(boolean useEncryption, boolean isClient) throws Exception {
    // Initialize the SSLContext to work with our key managers.
    SSLContext sslContext=SSLContext.getInstance(TLS_VERSIONS[0]);
    sslContext.init(keyManagers, trustManagers, null);
    SSLEngine sslEngine = sslContext.createSSLEngine();
    sslEngine.setUseClientMode(isClient);
    sslEngine.setNeedClientAuth(true);
    if(useEncryption){
      sslEngine.setEnabledCipherSuites(ENCRYPTED_CIPHER_SUITE);
    }
    else{
      sslEngine.setEnabledCipherSuites(NON_ENCRYPTED_CIPHER_SUITE);
    }
    sslEngine.setEnabledProtocols(TLS_VERSIONS);
    return new SslHandler(sslEngine);
  }
}
