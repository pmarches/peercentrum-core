package org.peercentrum.dht.selfregistration;

import org.peercentrum.dht.DHTClient;
import org.peercentrum.network.NetworkClient;

public class SelfRegistrationClient extends DHTClient {
  public SelfRegistrationClient(NetworkClient networkClient) {
    super(networkClient, SelfRegistrationDHT.APP_ID);
  }
}
