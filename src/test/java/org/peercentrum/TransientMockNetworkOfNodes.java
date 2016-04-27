package org.peercentrum;

import java.io.File;
import java.net.InetSocketAddress;

import org.bitcoinj.core.Wallet;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.ServerMain;
import org.peercentrum.core.TestUtilities;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.core.nodegossip.NodeGossipApplication;
import org.peercentrum.core.nodegossip.NodeGossipConfig;
import org.peercentrum.dht.selfregistration.SelfRegistrationDHT;
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.h2pk.HashToPublicKeyConfig;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkClientConnection;
import org.peercentrum.network.NodeIdentity;
import org.peercentrum.settlement.SettlementApplication;
import org.peercentrum.settlement.SettlementApplicationClient;
import org.peercentrum.settlement.SettlementConfig;
import org.peercentrum.settlement.SettlementDB;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;

public class TransientMockNetworkOfNodes {
  public TopLevelConfig server1Config;
  public ServerMain server1;

  public TopLevelConfig client1Config;
  public NetworkClient networkClient1;
  public BitcoinJSONRPCClient bitcoind = TestUtilities.getBitcoindRegtest();
  public NetworkClientConnection client1ToServer1Connection;
  public SettlementDB client1SettlementDB;
  public SettlementApplicationClient settlementClient1;
  public HashIdentifier helloWorldBlobID;
  public NodeIdentifier commonNodeId=new NodeIdentifier("12345678901234561234567890123456".getBytes());
  public NodeIdentifier serverSideOnlyNodeId=new NodeIdentifier("22345678901234561234567890123456".getBytes());

  public TransientMockNetworkOfNodes() throws Exception {
    //TODO make the different service lazy loaded to speed up tests
    configureServer();
    configureClient();

    configureClient1ToServer1Connection();

//    Runtime.getRuntime().addShutdownHook(new Thread(){
//      @Override
//      public void run() {
//        try {
//          shutdown();
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
//      }
//    });
  }

  private void configureClient1ToServer1Connection() throws Exception {
    client1ToServer1Connection=networkClient1.createConnectionToPeer(server1.getLocalIdentifier());
    settlementClient1=new SettlementApplicationClient(client1ToServer1Connection, client1Config, client1SettlementDB.settlementMethod);
  }

  private void configureServer() throws Exception {
    server1Config=generateConfiguration("serverNode1");
    server1=new ServerMain(server1Config);
    server1.getNodeDatabase().mapNodeIdToAddress(commonNodeId, InetSocketAddress.createUnresolved("commonNode.com", 1234));
    server1.getNodeDatabase().mapNodeIdToAddress(serverSideOnlyNodeId, InetSocketAddress.createUnresolved("serverOnlyNode.com", 1234));
    
    SettlementApplication settlementApp=new SettlementApplication(server1);
    new NodeGossipApplication(server1);
    new SelfRegistrationDHT(server1);

    File blobRepoDir=server1Config.getDirectory("blobRepo");
    P2PBlobRepositoryFS blobRepoFS=new P2PBlobRepositoryFS(blobRepoDir);
    P2PBlobApplication p2pBlobApp=new P2PBlobApplication(server1, blobRepoFS);
//    P2PBlobStoredBlob theBlob=new P2PBlobStoredBlobMemoryOnly("Hello world!".getBytes());
//    theBlob=blobRepoFS.createStoredBlob(theBlob.getHashList(), theBlob.getBlockLayout().getLengthOfBlob(), theBlob.getBlockLayout().getLengthOfEvenBlock());
//    theBlob=blobRepoFS.importBlobBytes("Hello world!".getBytes());
//    helloWorldBlobID=theBlob.getBlobIdentifier();
    File hellowWorldFile=new File(blobRepoDir, "Hello world.txt");
    Files.append("Hello world!", hellowWorldFile, Charsets.UTF_8);
    helloWorldBlobID=blobRepoFS.importFileIntoRepository(hellowWorldFile);
  }

  private void configureClient() throws Exception {
    client1Config=generateConfiguration("clientNode1");
    NodeDatabase clientNodeDatabase=new NodeDatabase(null);
    networkClient1=new NetworkClient(new NodeIdentity(client1Config), clientNodeDatabase);
    clientNodeDatabase.mapNodeIdToAddress(server1.getLocalIdentifier(), new InetSocketAddress(server1.getNetworkServer().getListeningPort()));
    clientNodeDatabase.mapNodeIdToAddress(commonNodeId, InetSocketAddress.createUnresolved("commonNode.com", 1234));

    client1SettlementDB=new SettlementDB(null);
  }

  public void shutdown() throws Exception{
    settlementClient1.close();
    client1SettlementDB.close();
    
    client1ToServer1Connection.close();
    
    server1.getNetworkServer().stopAcceptingConnections();
    networkClient1.close();
    TestUtilities.deleteDirectory(server1.getConfig().directoryOfConfigFile);
    TestUtilities.deleteDirectory(client1Config.directoryOfConfigFile);
  }

  public void fundBitcoinWalletOfNode(Wallet walletToFund, double fundingAmount) throws Exception{
//    ListenableFuture<Coin> balanceFuture = walletToFund.getBalanceFuture(amountPlusFee, Wallet.BalanceType.AVAILABLE);
//    if (!balanceFuture.isDone()) {
      String clientAddress=walletToFund.currentReceiveAddress().toString();
      bitcoind.sendToAddress(clientAddress, fundingAmount);
      bitcoind.setGenerate(true);
//      Futures.getUnchecked(balanceFuture);  // Wait.
//    }
  }
 
  private TopLevelConfig generateConfiguration(String nodeName) {
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
}