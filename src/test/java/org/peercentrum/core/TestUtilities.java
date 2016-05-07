package org.peercentrum.core;

import java.io.File;

public class TestUtilities {
  public static TopLevelConfig getConfig(String whatNode) throws Exception{
    File preparedConfigFile=new File("testdata/"+whatNode+"/peercentrum-config.yaml");
    if(preparedConfigFile.exists()==false){
      throw new Exception("Unknown node '"+whatNode+"'");
    }
    return TopLevelConfig.loadFromFile(preparedConfigFile);
  }
  
  static public void deleteDirectory(File path) {
    if (path == null){
      return;
    }
    if (path.exists()) {
      for(File f : path.listFiles()) {
        if(f.isDirectory()) {
          deleteDirectory(f);
          f.delete();
        }
        else {
          f.delete();
        }
      }
      path.delete();
    }
  }
}
