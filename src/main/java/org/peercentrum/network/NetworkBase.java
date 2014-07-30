package org.peercentrum.network;

import org.peercentrum.core.NodeIdentifier;

public class NetworkBase {
  protected NodeIdentity nodeIdentity;

  public NetworkBase(NodeIdentity localIdentity) throws Exception {
    this.nodeIdentity=localIdentity;
  }

  public NodeIdentifier getNodeIdentifier(){
    return nodeIdentity.getIdentifier();
  }

}
