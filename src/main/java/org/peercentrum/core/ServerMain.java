package org.peercentrum.core;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepository;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);
	
	TopLevelConfig config;
	NetworkServer server;

	public ServerMain(TopLevelConfig configNode) {
		this.config=configNode;
	}

	public static void main(String[] args) throws Exception {
		if(args.length!=1){
			System.err.println("Usage : "+ServerMain.class.getSimpleName()+" <configFile.yaml>");
			return;
		}
		TopLevelConfig config = TopLevelConfig.loadFromFile(new File(args[0]));
		config.setEnableNAT(true);
		ServerMain serverMain = new ServerMain(config);
		serverMain.run();
		Thread.sleep(Long.MAX_VALUE);
	}

	public void run() {
		try {
			NodeGossipConfig gossipConfig=(NodeGossipConfig) config.getAppConfig(NodeGossipConfig.class);

			NodeDatabase nodeDb=new NodeDatabase(gossipConfig.getNodeDatabasePath());
			NodeIdentifier serverId=new NodeIdentifier(config.getNodeIdentifier());
			server = new NetworkServer(serverId, nodeDb, config.getListenPort());
			server.setConfig(config);
			if(config.getEnableNAT()){
				enableNATInboundConnections();
			}

			//TODO Load the applications from the config file, dynamically, resolving dependencies, ... Maybe we need a OSGI container now?
			new NodeGossipApplication(server);
			P2PBlobConfig blobConfig=(P2PBlobConfig) config.getAppConfig(P2PBlobConfig.class);
			Path repositoryPath = FileSystems.getDefault().getPath(blobConfig.getBlobRepositoryPath());
			P2PBlobRepository blobRepository=new P2PBlobRepositoryFS(repositoryPath);
			new P2PBlobApplication(server, blobRepository);
			
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					try {
						server.stopAcceptingConnections();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Exception in main", e);
		}
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
//		server.setListeningAddress(externalIPAddress);

		final int localPortToMap=server.getListeningPort();
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

}
