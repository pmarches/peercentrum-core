package org.peercentrum.application;

import org.peercentrum.core.ServerMain;

public class PeercentrumApplication {
  protected ApplicationLifecycleHandler applicationLifecycleHandler;
  protected ConnectionLifecycleHandler connectionLifecycleHandler;
  
  public PeercentrumApplication(ServerMain serverMain){
    applicationLifecycleHandler=new ApplicationLifecycleHandler(serverMain);
    connectionLifecycleHandler=new ConnectionLifecycleHandler(serverMain);
  }
}
