package org.peercentrum.core;

import java.io.File;
import java.net.InetSocketAddress;

import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobConfig;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.h2pk.HashToPublicKeyConfig;
import org.peercentrum.network.NetworkClient;
import org.peercentrum.network.NetworkServer;
import org.peercentrum.settlement.SettlementApplication;
import org.peercentrum.settlement.SettlementConfig;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;
import com.azazar.bitcoin.jsonrpcclient.BitcoinJSONRPCClient;
import com.google.bitcoin.core.Wallet;
import com.google.common.io.Files;

public class TransientMockNetworkOfNodes {
  public TopLevelConfig server1Config;
  public NetworkServer server1;

  public TopLevelConfig client1Config;
  public NetworkClient networkClient1;
  public BitcoinJSONRPCClient bitcoind = TestUtilities.getBitcoindRegtest();

  public TransientMockNetworkOfNodes() throws Exception {
    server1Config=generateConfiguration("serverNode1");
    server1=new NetworkServer(server1Config);
    SettlementApplication settlementApp=new SettlementApplication(server1);
    P2PBlobRepositoryFS blobRepoFS=new P2PBlobRepositoryFS(server1Config.getDirectory("blobRepo"));
    P2PBlobApplication p2pBlobApp=new P2PBlobApplication(server1, blobRepoFS);

    
    client1Config=generateConfiguration("clientNode1");
    NodeDatabase clientNodeDatabase=new NodeDatabase(null);
    networkClient1=new NetworkClient(new NodeIdentifier(client1Config.getNodeIdentifier()), clientNodeDatabase);
    networkClient1.getNodeDatabase().mapNodeIdToAddress(server1.getLocalNodeId(), new InetSocketAddress(server1.getListeningPort()));
    
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

  public void shutdown() throws Exception{
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
    generatedConfig.setAppConfig(new P2PBlobConfig());
    generatedConfig.setAppConfig(new HashToPublicKeyConfig());
    File generatedBaseDir=Files.createTempDir();
    generatedConfig.setBaseDirectory(generatedBaseDir);
    //TODO Generate P2PBlob repo content
    //TODO Create bitcoin wallet and fund it
    //
    return generatedConfig;
  }
}