package org.peercentrum.core;

import java.io.File;
import java.net.InetAddress;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepository;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.core.nodegossip.NodeGossipApplication;
import org.peercentrum.network.NetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);
	
	TopLevelConfig topConfig;
	NetworkServer server;

	public ServerMain(TopLevelConfig configNode) {
		this.topConfig=configNode;
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
			server = new NetworkServer(topConfig);
			server.setConfig(topConfig);
			if(topConfig.getEnableNAT()){
				enableNATInboundConnections();
			}

			startApplications();
			
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

  private void startApplications() throws Exception {
    //TODO Load the applications from the topConfig file, dynamically, resolving dependencies, ... Maybe we need a OSGI container now?
    //TODO Add application lifecycle OSGI or custom...
    new NodeGossipApplication(server);

    P2PBlobConfig blobConfig=(P2PBlobConfig) topConfig.getAppConfig(P2PBlobConfig.class);
    File repositoryPath = topConfig.getFileRelativeFromConfigDirectory("blobRepository");
    P2PBlobRepository blobRepository=new P2PBlobRepositoryFS(repositoryPath);
    new P2PBlobApplication(server, blobRepository);
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
