package org.peercentrum;

import java.io.File;

import org.peercentrum.core.ServerMain;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.dht.selfregistration.SelfRegistrationDHT;
import org.tmatesoft.sqljet.core.SqlJetException;

public class PermanentMockNetwork {
  private static final int NB_MOCK_NODES = 6;
  public ServerMain server[]=new ServerMain[NB_MOCK_NODES];
  private SelfRegistrationDHT[] dht=new SelfRegistrationDHT[NB_MOCK_NODES];
  public static final byte[] STORED_KEY1="12345678901234567890123456789012".getBytes();
  public static final byte[] STORED_VALUE1 = "Hello world".getBytes();
  
  public PermanentMockNetwork() throws Exception {
    for(int i=0; i<NB_MOCK_NODES; i++){
      TopLevelConfig config=TopLevelConfig.loadFromFile(new File("permanentMockNetwork.testdata/node"+(i+1)+"/peercentrum-config.yaml"));
      server[i]=new ServerMain(config);
      server[i].getNodeDatabase().reset();
    }
    
    configureLinksBetweenNodes();
    for(int i=0; i<NB_MOCK_NODES; i++){
      dht[i]=new SelfRegistrationDHT(server[i]); //FIXME This is crap.
    }
    configureDHTValues();
  }

  private void configureDHTValues() throws SqlJetException {
    dht[5].storeKeyValue(STORED_KEY1, STORED_VALUE1, 0);
  }

  private void configureLinksBetweenNodes() {
    server[0].getNodeDatabase().mapNodeIdToAddress(server[1].getNodeIdentifier(), server[1].getNetworkServer().getListeningAddress());
    server[1].getNodeDatabase().mapNodeIdToAddress(server[0].getNodeIdentifier(), server[0].getNetworkServer().getListeningAddress());

    server[1].getNodeDatabase().mapNodeIdToAddress(server[2].getNodeIdentifier(), server[2].getNetworkServer().getListeningAddress());
    server[2].getNodeDatabase().mapNodeIdToAddress(server[3].getNodeIdentifier(), server[3].getNetworkServer().getListeningAddress());
    server[3].getNodeDatabase().mapNodeIdToAddress(server[4].getNodeIdentifier(), server[4].getNetworkServer().getListeningAddress());
    server[3].getNodeDatabase().mapNodeIdToAddress(server[5].getNodeIdentifier(), server[5].getNetworkServer().getListeningAddress());
  }
}
