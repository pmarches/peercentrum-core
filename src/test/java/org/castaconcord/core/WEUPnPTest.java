package org.castaconcord.core;

import static org.junit.Assert.fail;

import java.net.InetAddress;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.junit.Test;

public class WEUPnPTest {

	private static final int SAMPLE_PORT = 8081;
	private static final int WAIT_TIME = 30;

	@Test
	public void test() throws Exception {
		GatewayDiscover discover = new GatewayDiscover();
		System.out.println("Looking for Gateway Devices");
		discover.discover();
		GatewayDevice d = discover.getValidGateway();

		if (null != d) {
			System.out.println("Gateway device found "+d.getModelName()+" " +d.getModelDescription());
		} else {
			System.out.println("No valid gateway device found.");
			return;
		}

		InetAddress localAddress = d.getLocalAddress();
		System.out.println("Using local address: "+ localAddress);
		String externalIPAddress = d.getExternalIPAddress();
		System.out.println("External address: "+ externalIPAddress);
		PortMappingEntry portMapping = new PortMappingEntry();
		
		System.out.println("Attempting to map port {0}" + SAMPLE_PORT);
		System.out.println("Querying device to see if mapping for port {0} already exists"+	SAMPLE_PORT);

		if (!d.getSpecificPortMappingEntry(SAMPLE_PORT, "TCP", portMapping)) {
			System.out.println("Sending port mapping request");

			if (d.addPortMapping(SAMPLE_PORT, SAMPLE_PORT, localAddress.getHostAddress(), "TCP", "libminiupnpc")) {
				System.out.println("Mapping succesful: waiting {0} seconds before removing mapping." + WAIT_TIME);

				Thread.sleep(1000 * WAIT_TIME);
				d.deletePortMapping(SAMPLE_PORT, "TCP");

				System.out.println("Port mapping removed");
				System.out.println("Test SUCCESSFUL");
			} else {
				System.out.println("Port mapping failed");
				fail();
			}

		} else {
			System.out.println("Port was already mapped. Aborting test.");
			fail();
		}

		System.out.println("Stopping weupnp");
	}

}
