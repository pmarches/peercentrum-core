package org.peercentrum.application;

import org.peercentrum.core.ServerMain;

public class ApplicationLifecycleHandler {
  enum ApplicationState {
    STOPPED,
    STARTING,
    STARTED,
  }
  protected ApplicationState currentApplicationState=ApplicationState.STOPPED;
  protected ServerMain serverMain;

  public ApplicationLifecycleHandler(ServerMain serverMain){
    this.serverMain=serverMain;
  }
  
  synchronized public void changeState(ApplicationState newState){
    boolean isStateChangeLegal=currentApplicationState==ApplicationState.STARTED && newState==ApplicationState.STOPPED;
    isStateChangeLegal|=currentApplicationState.ordinal()+1!=newState.ordinal();
    if(isStateChangeLegal==false){
      throw new IllegalArgumentException("Cannot change state from "+currentApplicationState+" to "+newState);
    }

    onBeforeStateChange(newState);

    ApplicationState previousState=currentApplicationState;
    currentApplicationState=newState;

    onAfterStateChange(previousState);
  }
  
  public void onBeforeStateChange(ApplicationState newState){}
  public void onAfterStateChange(ApplicationState previousState){}
}
