package org.peercentrum.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.peercentrum.blob.P2PBlobApplication;
import org.peercentrum.blob.P2PBlobRepositoryFS;
import org.peercentrum.blob.P2PBlobStandaloneClient;
import org.peercentrum.blob.P2PBlobStoredBlob;
import org.peercentrum.blob.P2PBlobStoredBlobMemoryOnly;
import org.peercentrum.core.NodeDatabase;
import org.peercentrum.core.NodeIdentifier;
import org.peercentrum.core.TopLevelConfig;
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.network.NetworkServer;

public class P2PBlobApplicationTest {

	@Test
	public void test() throws Exception {
		TopLevelConfig node1Config=new TopLevelConfig("Node1");
		NetworkServer node1 = new NetworkServer(node1Config);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", node1.getListeningPort());
		NodeDatabase sharedNodeDatabase = node1.getNodeDatabase();
		sharedNodeDatabase.mapNodeIdToAddress(node1.getLocalNodeId(), serverEndpoint);

		Path repositoryPath = FileSystems.getDefault().getPath("testRepo");
		P2PBlobRepositoryFS serverSideBlobRepo=new P2PBlobRepositoryFS(repositoryPath);
		P2PBlobApplication serverSideApp=new P2PBlobApplication(node1, serverSideBlobRepo);
		
		NodeIdentifier clientNodeId=new NodeIdentifier("ClientNode");
		P2PBlobStandaloneClient client = new P2PBlobStandaloneClient(node1.getLocalNodeId(), clientNodeId, sharedNodeDatabase);
		HashIdentifier blobIdToDownload=new HashIdentifier(DatatypeConverter.parseHexBinary("B67D1B1F9C750304D9E8A63CD3C077B5C9AC6131BB2C2C874CEAFE53AC69F5F9"));
		P2PBlobStoredBlobMemoryOnly download=new P2PBlobStoredBlobMemoryOnly(blobIdToDownload);
		Future<P2PBlobStoredBlob> downloadCompleteFuture=client.downloadAll(download);
		downloadCompleteFuture.sync();
		assertNotNull(download.getHashList()); //By default we will download the full blob's hash list
		assertEquals(761_996, download.downloadedAndValidatedBlobContent.readableBytes());
		assertEquals(761_996, download.blobLengthInBytes);
		client.close();
//		Thread.sleep(3000);
	}

}
