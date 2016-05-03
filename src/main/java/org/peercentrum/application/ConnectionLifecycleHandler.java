package org.peercentrum.application;

import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ServerMain;

public class ConnectionLifecycleHandler {
  protected ServerMain serverMain;
  
  public ConnectionLifecycleHandler(ServerMain serverMain) {
    this.serverMain=serverMain;
  }

  /***
   * TODO We might be able to veto a connection here?
   */
  public void onBeforeConnectToNode(NodeIdentifier remoteNodeId){}
  public void onAfterConnectedToNode(Object connectionToRemoteNode){} //Called for both outgoing and incoming connections
  
  public void onBeforeDisconnectFromNode(Object connectionToRemoteNode){}
  public void onAfterDisconnectFromNode(NodeIdentifier remoteNodeId){}

}
