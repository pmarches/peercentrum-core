package org.peercentrum.settlement;

import java.math.BigInteger;

import org.peercentrum.core.NodeIdentifier;

public interface SettleableApplication {
  public void creditNode(NodeIdentifier nodeId, BigInteger btcAmount);
}
