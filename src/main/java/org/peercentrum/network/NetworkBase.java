package org.peercentrum.network;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import io.netty.util.internal.EmptyArrays;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(NetworkBase.class);
  
  protected TrustManagerFactory myTrustManagerFactory;
  protected NodeIdentity nodeIdentity;

  public NetworkBase(NodeIdentity localIdentity) throws Exception {
    myTrustManagerFactory = new MyTrustManagerFactory();
    this.nodeIdentity=localIdentity;
  }


  public NodeIdentifier getNodeIdentifier(){
    return nodeIdentity.getIdentifier();
  }

  class MyTrustManagerFactory extends SimpleTrustManagerFactory {
    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {
      LOGGER.debug("Engine init ks="+keyStore);
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
      LOGGER.debug("Engine init "+managerFactoryParameters);
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
      return new TrustManager[]{new VerboseTrustManager()};
    }
  };

  class VerboseTrustManager implements X509TrustManager {
    private X509Certificate[] receivedCerts;

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      LOGGER.debug("Check Client trusted "+Arrays.asList(chain)+" "+authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      LOGGER.debug("Check Server trusted "+Arrays.asList(chain)+" "+authType);
      receivedCerts=chain;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      LOGGER.debug("getAcceptedIssuers");
//      return receivedCerts;
      return EmptyArrays.EMPTY_X509_CERTIFICATES;
    }
  };

}
