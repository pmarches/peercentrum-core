package org.peercentrum.core;

import java.io.File;
import java.util.Hashtable;

import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepository;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.core.nodegossip.NodeGossipApplication;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.NetworkApplication;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;
import org.peercentrum.network.NodeIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain implements Runnable {
	public static final String BLOB_REPOSITORY_DIRNAME = "blobRepository";

  public static final String NODE_DB_FILENAME = "node.db";

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);
	
	protected TopLevelConfig topConfig;
  protected NodeIdentity nodeIdentity;
	protected NetworkServer networkServer;
  protected NetworkClient networkClient;
  protected NodeDatabase nodeDatabase;
  protected Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler> allApplicationHandler=new Hashtable<ApplicationIdentifier, BaseApplicationMessageHandler>();

	public ServerMain(TopLevelConfig configNode) throws Exception {
		this.topConfig=configNode;
		nodeIdentity=new NodeIdentity(topConfig);

		this.nodeDatabase=new NodeDatabase(topConfig.getFileRelativeFromConfigDirectory(NODE_DB_FILENAME));

    networkServer = new NetworkServer(this);

		networkClient=new NetworkClient(getLocalIdentity(), nodeDatabase);
    networkClient.setLocalListeningPort(networkServer.getListeningPort());
    networkClient.useEncryption=topConfig.encryptConnection;
	}

  public static void main(String[] args) throws Exception {
		if(args.length!=1){
			System.err.println("Usage : "+ServerMain.class.getSimpleName()+" <configFile.yaml>");
			return;
		}
		TopLevelConfig config = TopLevelConfig.loadFromFile(new File(args[0]));
		ServerMain serverMain = new ServerMain(config);
		serverMain.run();
		Thread.sleep(Long.MAX_VALUE);
	}

	public void run() {
		try {
			startApplications();
			
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					try {
						networkServer.stopAcceptingConnections();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			LOGGER.error("Exception in main", e);
		}
	}

  public void startApplications() throws Exception {
    //TODO Load the applications from the topConfig file, dynamically, resolving dependencies, ... Maybe we need a OSGI container now?
    //TODO Add application lifecycle OSGI or custom...
    new Thread(new NodeGossipApplication(this)).start();

    P2PBlobConfig blobConfig=(P2PBlobConfig) topConfig.getAppConfig(P2PBlobConfig.class);
    File repositoryPath = topConfig.getFileRelativeFromConfigDirectory(BLOB_REPOSITORY_DIRNAME);
    P2PBlobRepository blobRepository=new P2PBlobRepositoryFS(repositoryPath);
    new P2PBlobApplication(this, blobRepository);
  }

	
	
  public NodeDatabase getNodeDatabase(){
    return nodeDatabase;
  }

  public TopLevelConfig getConfig() {
    return topConfig;
  }

  public void addApplicationHandler(BaseApplicationMessageHandler applicationHandler) {
    this.allApplicationHandler.put(applicationHandler.getApplicationId(), applicationHandler);
  }

  public BaseApplicationMessageHandler getApplicationHandler(ApplicationIdentifier appIdReceived) {
    return allApplicationHandler.get(appIdReceived);
  }

  public NetworkServer getNetworkServer() {
    return networkServer;
  }

  public NetworkClient getNetworkClient() {
    return networkClient;
  }

  public NodeIdentity getLocalIdentity() {
    return nodeIdentity;
  }

  public NodeIdentifier getLocalIdentifier() {
    return nodeIdentity.getIdentifier();
  }



}
