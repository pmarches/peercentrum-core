package org.peercentrum.core;

import java.io.File;
import java.net.InetSocketAddress;

import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.h2pk.HashToPublicKeyConfig;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.peercentrum.network.NetworkServer;
import org.peercentrum.settlement.SettlementApplication;
import org.peercentrum.settlement.SettlementApplicationClient;
import org.peercentrum.settlement.SettlementConfig;
import org.peercentrum.settlement.SettlementDB;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;
import com.azazar.bitcoin.jsonrpcclient.BitcoinJSONRPCClient;
import com.google.bitcoin.core.Wallet;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class TransientMockNetworkOfNodes {
  public TopLevelConfig server1Config;
  public NetworkServer server1;

  public TopLevelConfig client1Config;
  public NetworkClient networkClient1;
  public BitcoinJSONRPCClient bitcoind = TestUtilities.getBitcoindRegtest();
  public NetworkClientConnection clientToServerConnection;
  public SettlementDB client1SettlementDB;
  public SettlementApplicationClient settlementClient1;
  public HashIdentifier helloWorldBlobID;

  public TransientMockNetworkOfNodes() throws Exception {
    //TODO make the different service lazy loaded to speed up tests
    configureServer();
    configureClient();

    configureClient1ToServer1Connection();

    Runtime.getRuntime().addShutdownHook(new Thread(){
      @Override
      public void run() {
        try {
          shutdown();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private void configureClient1ToServer1Connection() {
    clientToServerConnection=networkClient1.createConnectionToPeer(server1.getLocalNodeId());
    settlementClient1=new SettlementApplicationClient(clientToServerConnection, client1Config, client1SettlementDB.settlementMethod);
  }

  private void configureServer() throws InterruptedException, Exception {
    server1Config=generateConfiguration("serverNode1");
    server1=new NetworkServer(server1Config);

    SettlementApplication settlementApp=new SettlementApplication(server1);

    File blobRepoDir=server1Config.getDirectory("blobRepo");

    P2PBlobRepositoryFS blobRepoFS=new P2PBlobRepositoryFS(blobRepoDir);
    P2PBlobApplication p2pBlobApp=new P2PBlobApplication(server1, blobRepoFS);

    File hellowWorldFile=new File(blobRepoDir, "Hello world.txt");
    Files.append("Hello world!", hellowWorldFile, Charsets.UTF_8);
    helloWorldBlobID=blobRepoFS.importFileIntoRepository(hellowWorldFile);
  }

  private void configureClient() throws Exception {
    client1Config=generateConfiguration("clientNode1");
    NodeDatabase clientNodeDatabase=new NodeDatabase(null);
    networkClient1=new NetworkClient(new NodeIdentifier(client1Config.getNodeIdentifier()), clientNodeDatabase);
    networkClient1.getNodeDatabase().mapNodeIdToAddress(server1.getLocalNodeId(), new InetSocketAddress(server1.getListeningPort()));

    client1SettlementDB=new SettlementDB(null);
  }

  public void shutdown() throws Exception{
    settlementClient1.close();
    client1SettlementDB.close();
    
    clientToServerConnection.close();
    
    server1.stopAcceptingConnections();
    networkClient1.close();
    TestUtilities.deleteDirectory(server1.getConfig().directoryOfConfigFile);
    TestUtilities.deleteDirectory(client1Config.directoryOfConfigFile);
  }

  public void fundBitcoinWalletOfNode(Wallet walletToFund, double fundingAmount) throws BitcoinException{
//    ListenableFuture<Coin> balanceFuture = walletToFund.getBalanceFuture(amountPlusFee, Wallet.BalanceType.AVAILABLE);
//    if (!balanceFuture.isDone()) {
      String clientAddress=walletToFund.currentReceiveAddress().toString();
      bitcoind.sendToAddress(clientAddress, fundingAmount);
      bitcoind.setGenerate(true, 1);
//      Futures.getUnchecked(balanceFuture);  // Wait.
//    }
  }
 
  private TopLevelConfig generateConfiguration(String nodeName) {
    TopLevelConfig generatedConfig=new TopLevelConfig(nodeName);
    generatedConfig.setAppConfig(new SettlementConfig());
    generatedConfig.setAppConfig(new NodeGossipConfig());
    P2PBlobConfig p2pConfig=new P2PBlobConfig();
    p2pConfig.transferPricingPerGigabyte=new Integer(1);
    generatedConfig.setAppConfig(p2pConfig);
    generatedConfig.setAppConfig(new HashToPublicKeyConfig());
    File generatedBaseDir=Files.createTempDir();
    generatedConfig.setBaseDirectory(generatedBaseDir);
    //TODO Generate P2PBlob repo content
    //TODO Create bitcoin wallet and fund it
    //
    return generatedConfig;
  }
}