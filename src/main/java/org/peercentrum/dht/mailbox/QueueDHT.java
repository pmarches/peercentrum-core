package org.peercentrum.dht.mailbox;

import java.util.concurrent.TimeUnit;

import org.bouncycastle.util.Arrays;
import org.peercentrum.core.ApplicationIdentifier;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.PB;
import org.peercentrum.core.PB.DHTFindMsg;
import org.peercentrum.core.PB.DHTStoreValueMsg;
import org.peercentrum.core.PB.DHTTopLevelMsg.Builder;
import org.peercentrum.core.ServerMain;
import org.peercentrum.core.Signature;
import org.peercentrum.dht.DHTApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This application allows a anyone to append values to a specified key. Only the owner of the key
 * may remove elements from it.
 */
public class QueueDHT extends DHTApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueueDHT.class);
  public static ApplicationIdentifier APP_ID=new ApplicationIdentifier(QueueDHT.class.getName().getBytes());
  
  public QueueDHT(ServerMain serverMain) throws Exception {
    super(serverMain);
    setEntryTimeToLive(30, TimeUnit.DAYS);
    setEntryMaximumCardinality(1000);
    setEntryOverflowHandling(OverflowHandling.LIFO);
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
        return false;
      }
    } catch (Exception e) {
      LOGGER.error("", e);
      return false;
    }
    return true;
  }

  @Override
  protected void handleValidStoreMsg(Builder topLevelResponseMsg, DHTStoreValueMsg storeValue) throws Exception {
    // TODO Auto-generated method stub
    
  }

  @Override
  protected void handleFindMsg(Builder topLevelResponseMsg, DHTFindMsg find) throws Exception {
    // TODO Auto-generated method stub
    
  }


  @Override
  public ApplicationIdentifier getApplicationId() {
    return APP_ID;
  }

}
