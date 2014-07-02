package org.castaconcord.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

public class TopLevelConfig {
	File directoryOfConfigFile;	

	String nodeIdentifier; //FIXME Temporary, should be derived from the private key...
	String privateKey;
	int listenPort=0;
	List<?> applications;
	boolean enableNAT=false;

	public static TopLevelConfig loadFromFile(File file) throws Exception {
		Yaml yaml = new Yaml();
		TopLevelConfig config= (TopLevelConfig) yaml.load(new FileInputStream(file));
		config.setBaseDirectory(file.getAbsoluteFile().getParentFile().getCanonicalFile());
		return config;
	}
	
	public String getNodeIdentifier() {
		return nodeIdentifier;
	}
	public void setNodeIdentifier(String nodeIdentifier) {
		this.nodeIdentifier = nodeIdentifier;
	}

	public String getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
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
	public void setApplications(List<?> applications) {
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
		return "Config [nodeIdentifier=" + nodeIdentifier + ", privateKey=" + privateKey + ", listenPort="
				+ listenPort + ", applications=" + applications + "]";
	}
}
