package org.peercentrum.dht.selfregistration;

import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.Arrays;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.Signature;
import org.peercentrum.dht.DHTApplication;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This application allows a node to register itself with a chosen hash. No node 'owns' any key, but
 * a node cannot be registered/unregistered by anyone else.
 *
 * Usefull for stuff like swarm tracking like bitorrent.
 */
public class SelfRegistrationDHT extends DHTApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(SelfRegistrationDHT.class);
  public static ApplicationIdentifier APP_ID=new ApplicationIdentifier(SelfRegistrationDHT.class.getName().getBytes());
  
  public SelfRegistrationDHT(NetworkServer server) throws Exception {
    super(server);
    setEntryTimeToLive(1, TimeUnit.DAYS);
    setEntryMaximumCardinality(1000);
    setEntryOverflowHandling(OVERFLOW_HANDLING.LIFO); //LIFO or FIFO    
  }

  @Override
  public ApplicationIdentifier getApplicationId() {
    return APP_ID;
  }

  @Override
  public boolean isTransactionValid(PB.DHTStoreValueMsg storeValueMsg) {

    try {
      byte[] key=null;
      byte[] value=null;
      byte[] dataToVerify=Arrays.concatenate(key, value);
      Signature sig=new Signature(storeValueMsg.getSignature().toByteArray());
      NodeIdentifier signerId=new NodeIdentifier(key);
      if(sig.isSignatureValid(dataToVerify, signerId.getPublicKey())==false){
        LOGGER.error("Failed to validate signature of message {}", storeValueMsg);
      }
    } catch (Exception e) {
      LOGGER.error("", e);
      return false;
    }
    return true;
  }


}
