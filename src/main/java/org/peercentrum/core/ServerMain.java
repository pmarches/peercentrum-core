package org.peercentrum.core;

import java.io.File;
import java.net.InetAddress;
import java.util.Hashtable;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepository;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.core.nodegossip.NodeGossipApplication;
import org.peercentrum.network.BaseApplicationMessageHandler;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;
import org.peercentrum.network.NodeIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain implements Runnable {
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

//  this.nodeDatabase=new NodeDatabase(null);
		this.nodeDatabase=new NodeDatabase(topConfig.getFileRelativeFromConfigDirectory("node.db"));

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

			if(topConfig.getEnableNAT()){
				enableNATInboundConnections();
			}

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

  private void startApplications() throws Exception {
    //TODO Load the applications from the topConfig file, dynamically, resolving dependencies, ... Maybe we need a OSGI container now?
    //TODO Add application lifecycle OSGI or custom...
    new Thread(new NodeGossipApplication(this)).start();

    P2PBlobConfig blobConfig=(P2PBlobConfig) topConfig.getAppConfig(P2PBlobConfig.class);
    File repositoryPath = topConfig.getFileRelativeFromConfigDirectory("blobRepository");
    P2PBlobRepository blobRepository=new P2PBlobRepositoryFS(repositoryPath);
    new P2PBlobApplication(this, blobRepository);
  }

	
	protected void enableNATInboundConnections() throws Exception {
		GatewayDiscover discover = new GatewayDiscover();
		LOGGER.debug("Looking for gateway devices");
		discover.discover();
		final GatewayDevice natDevice = discover.getValidGateway();
		if (null != natDevice) {
			LOGGER.debug("Gateway device found {} {} ", natDevice.getModelName(), natDevice.getModelDescription());
		} else {
			LOGGER.debug("No valid gateway device found, doing nothing.");
			return;
		}

//		String externalIPAddress = natDevice.getExternalIPAddress();
//		LOGGER.debug("Our external address is {}", externalIPAddress);
//		server1.setListeningAddress(externalIPAddress);

		final int localPortToMap=networkServer.getListeningPort();
		LOGGER.debug("Querying device to see if a mapping for port {} already exists", localPortToMap);

		PortMappingEntry portMapping = new PortMappingEntry();
		if (natDevice.getSpecificPortMappingEntry(localPortToMap, "TCP", portMapping)) {
			LOGGER.error("Port {} was already mapped by {}. Aborting...", localPortToMap, portMapping.getPortMappingDescription()); //TODO Maybe we should retry with another port number. Do not forget to update the local gossip info with the new port
		} else {
			LOGGER.debug("External port {} is available, sending mapping request", localPortToMap);
			InetAddress localAddress = natDevice.getLocalAddress();
			if (natDevice.addPortMapping(localPortToMap, localPortToMap, localAddress.getHostAddress(), "TCP", getClass().getName())) {
				LOGGER.info("Port mapping successfull");
				Runtime.getRuntime().addShutdownHook(new Thread(){
					public void run() {
						try {
							LOGGER.debug("deleting port mapping {}", localPortToMap);
							natDevice.deletePortMapping(localPortToMap, "TCP");
						} catch (Exception e) {
							e.printStackTrace();
						}
					};
				});
			} else {
				LOGGER.error("Port mapping failed");
			}
		}
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

  public NodeIdentifier getNodeIdentifier() {
    return nodeIdentity.getIdentifier();
  }



}
