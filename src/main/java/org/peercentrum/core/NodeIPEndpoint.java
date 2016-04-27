package org.peercentrum.core;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeIPEndpoint {
  private static final Logger LOGGER = LoggerFactory.getLogger(NodeIPEndpoint.class);
  
  protected NodeIdentifier nodeId;
  protected InetSocketAddress tcpAddress;
  
  public NodeIPEndpoint(NodeIdentifier nodeIdentifier, InetSocketAddress inetSocketAddress) {
    this.nodeId=nodeIdentifier;
    this.tcpAddress=inetSocketAddress;
  }

  public static NodeIPEndpoint parseFromString(String bootstrapEndpoint){
    String[] nodeIDAddressAndPort=bootstrapEndpoint.split(":");
    if(nodeIDAddressAndPort==null || nodeIDAddressAndPort.length!=3){
      LOGGER.error("bootstrap entry has not been recognized {}", bootstrapEndpoint);
      return null;
    }
    
    NodeIPEndpoint endpoint=new NodeIPEndpoint(new NodeIdentifier(nodeIDAddressAndPort[0]),
            new InetSocketAddress(nodeIDAddressAndPort[1], 
            Integer.parseInt(nodeIDAddressAndPort[2])));
    return endpoint;
  }

  public NodeIdentifier getNodeId() {
    return nodeId;
  }
  
  @Override
  public String toString() {
    return nodeId.toString()+":"+tcpAddress.toString();
  }

  public InetSocketAddress getAddress() {
    return tcpAddress;
  }
}
