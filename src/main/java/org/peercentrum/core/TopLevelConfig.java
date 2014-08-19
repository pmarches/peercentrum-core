package org.peercentrum.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

public class TopLevelConfig {
  public File directoryOfConfigFile;	

  int listenPort=0;
  List<Object> applications=new ArrayList<>();
  boolean enableNAT=false;
  public boolean encryptConnection=true;
  
  public static TopLevelConfig loadFromFile(File file) throws Exception {
    Yaml yaml = new Yaml();
    TopLevelConfig config= (TopLevelConfig) yaml.load(new FileInputStream(file));
    config.setBaseDirectory(file.getAbsoluteFile().getParentFile().getCanonicalFile());
    return config;
  }

  public int getListenPort() {
    return listenPort;
  }
  public void setListenPort(int listenPort) {
    this.listenPort = listenPort;
  }

  public List<?> getApplications() {
    return applications;
  }
  public void setApplications(List<Object> applications) {
    this.applications = applications;
  }


  public Object getAppConfig(Class<?> classToFind) {
    for(Object appConfig : applications){
      if(classToFind.isInstance(appConfig)){
        return appConfig;
      }
    }
    return null;
  }

  public void setAppConfig(Object appConfig) {
    this.applications.add(appConfig);
  }


  public void setBaseDirectory(File directoryOfConfigFile) {
    this.directoryOfConfigFile=directoryOfConfigFile;
  }
  public File getBaseDirectory(){
    return directoryOfConfigFile;
  }

  public void setEnableNAT(boolean enableNAT) {
    this.enableNAT=enableNAT;
  }
  public boolean getEnableNAT() {
    return this.enableNAT;
  }

  @Override
  public String toString() {
    return "Config [listenPort="
        + listenPort + ", applications=" + applications + "]";
  }

  public File getFile(String fileName) {
    if(directoryOfConfigFile==null || fileName==null){
      throw new NullPointerException("The config file directory must have been initialized");
    }
    return new File(directoryOfConfigFile, fileName);
  }

  public File getDirectory(String string) {
    File directoryGenerated=getFile(string);
    directoryGenerated.mkdirs();
    return directoryGenerated;
  }

}
