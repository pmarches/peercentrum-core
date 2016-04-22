package org.peercentrum.dht.mailbox;

import org.peercentrum.dht.DHTClient;
import org.peercentrum.network.NetworkClient;

public class QueueClient extends DHTClient {
  public QueueClient(NetworkClient networkClient) {
    super(networkClient, QueueDHT.APP_ID);
  }
}
