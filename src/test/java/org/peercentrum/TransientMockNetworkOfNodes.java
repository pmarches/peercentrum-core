package org.peercentrum;

import java.io.File;
import java.net.InetSocketAddress;

import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepository;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.blob.P2PBlobStandaloneClient;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ServerMain;
import org.peercentrum.core.TestUtilities;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.core.nodegossip.NodeGossipApplication;
import org.peercentrum.core.nodegossip.NodeGossipConfig;
import org.peercentrum.h2pk.HashToPublicKeyConfig;
import org.peercentrum.network.NetworkApplication;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientTCPConnection;
import org.peercentrum.network.NodeIdentity;
import org.peercentrum.nodestatistics.NodeStatisticsDatabase;
import org.peercentrum.settlement.SettlementConfig;

import com.google.common.io.Files;

public class TransientMockNetworkOfNodes {
  static final NodeIdentifier commonNodeId=new NodeIdentifier("11111111111111111111111111111111".getBytes());
  static final NodeIdentifier serverSideOnlyNodeId=new NodeIdentifier("22222222222222222222222222222222".getBytes());

  public ServerMain server1;
  public NetworkClient networkClient1;

  public TopLevelConfig client1Config;
  public NetworkClientTCPConnection client1ToServer1Connection;
  
  public TransientMockNetworkOfNodes() {
    try{
      //TODO make the different service lazy loaded to speed up tests
      configureServer();
      configureClient();
  
      configureClient1ToServer1Connection();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void configureClient1ToServer1Connection() throws Exception {
    client1ToServer1Connection=networkClient1.createConnectionToPeer(server1.getLocalIdentifier());
  }

  private void configureServer() throws Exception {
    server1=build("serverNode1");

    server1.getNodeDatabase().mapNodeIdToAddress(commonNodeId, InetSocketAddress.createUnresolved("commonNode.com", 1234));
    server1.getNodeDatabase().mapNodeIdToAddress(serverSideOnlyNodeId, InetSocketAddress.createUnresolved("serverOnlyNode.com", 1234));
  }

  public ServerMain build(String nodeName) throws Exception {
    TopLevelConfig topConfig = generateConfiguration(nodeName);
    ServerMain server=new ServerMain(topConfig);
    server.startApplications();
    return server;
  }

  public TopLevelConfig generateConfiguration(String nodeName) {
    TopLevelConfig generatedConfig=new TopLevelConfig();
    generatedConfig.setAppConfig(new SettlementConfig());
    generatedConfig.setAppConfig(new NodeGossipConfig());
    P2PBlobConfig p2pConfig=new P2PBlobConfig();
    p2pConfig.transferPricingPerGigabyte=new Integer(1);
    generatedConfig.setAppConfig(p2pConfig);
    generatedConfig.setAppConfig(new HashToPublicKeyConfig());
    File generatedBaseDir=Files.createTempDir();
    generatedConfig.setBaseDirectory(generatedBaseDir);
    return generatedConfig;
  }
  
//  public void fundBitcoinWalletOfNode(Wallet walletToFund, double fundingAmount) throws Exception{
////  ListenableFuture<Coin> balanceFuture = walletToFund.getBalanceFuture(amountPlusFee, Wallet.BalanceType.AVAILABLE);
////  if (!balanceFuture.isDone()) {
//  if(bitcoind==null){
//    bitcoind = TestUtilities.getBitcoindRegtest();
//  }
//    String clientAddress=walletToFund.currentReceiveAddress().toString();
//    bitcoind.sendToAddress(clientAddress, fundingAmount);
//    bitcoind.setGenerate(true);
////    Futures.getUnchecked(balanceFuture);  // Wait.
////  }
//}
  
  private void configureClient() throws Exception {
    client1Config = generateConfiguration("clientNode1");
    NodeStatisticsDatabase clientNodeDatabase=new NodeStatisticsDatabase(null);
    networkClient1=new NetworkClient(new NodeIdentity(client1Config), clientNodeDatabase);
    clientNodeDatabase.mapNodeIdToAddress(server1.getLocalIdentifier(), new InetSocketAddress(server1.getNetworkServer().getListeningPort()));
    clientNodeDatabase.mapNodeIdToAddress(commonNodeId, InetSocketAddress.createUnresolved("commonNode.com", 1234));
  }

  public void shutdown() throws Exception{
    client1ToServer1Connection.close();
    
    server1.getNetworkServer().stopAcceptingConnections();
    networkClient1.close();
    TestUtilities.deleteDirectory(server1.getConfig().directoryOfConfigFile);
    TestUtilities.deleteDirectory(client1Config.directoryOfConfigFile);
  }
 
}