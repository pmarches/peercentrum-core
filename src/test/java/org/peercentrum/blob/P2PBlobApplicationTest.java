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
import org.peercentrum.h2pk.HashIdentifier;
import org.peercentrum.network.NetworkServer;

public class P2PBlobApplicationTest {

	@Test
	public void test() throws Exception {
		NodeIdentifier clientNodeId=new NodeIdentifier("ClientNode");
		NodeDatabase sharedNodeDatabase = new NodeDatabase(null);
		NodeIdentifier nodeId=new NodeIdentifier("Node1");
		NetworkServer nodeServer = new NetworkServer(nodeId, sharedNodeDatabase, 0);
		InetSocketAddress serverEndpoint=new InetSocketAddress("localhost", nodeServer.getListeningPort());
		sharedNodeDatabase.mapNodeIdToAddress(nodeId, serverEndpoint);

		Path repositoryPath = FileSystems.getDefault().getPath("testRepo");
		P2PBlobRepositoryFS serverSideBlobRepo=new P2PBlobRepositoryFS(repositoryPath);
		P2PBlobApplication serverSideApp=new P2PBlobApplication(nodeServer, serverSideBlobRepo);
		
		P2PBlobStandaloneClient client = new P2PBlobStandaloneClient(nodeId, clientNodeId, sharedNodeDatabase);
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
