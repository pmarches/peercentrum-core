package org.peercentrum;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.peercentrum.blob.P2PBlobApplicationTest;
import org.peercentrum.network.NetworkClientTest;

@RunWith(Suite.class)
@SuiteClasses({NetworkClientTest.class, P2PBlobApplicationTest.class})
public class FullSuite {
}
