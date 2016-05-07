package org.peercentrum.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class OSGIActivator implements BundleActivator {

  public void start(BundleContext context) throws Exception {
    installShutdownHook(context);
  }

  private void installShutdownHook(BundleContext context) {
    System.out.println("Installing shutdown hooks");
    Thread shutdownHook = new Thread() {
      public void run() {
        try {
          System.out.println("ShutdownHook triggered");
          context.getBundle().stop();
        } catch (IllegalStateException e) {
          System.err.println("This bundle likely did not start correctly anyways");
        } catch (BundleException e) {
          e.printStackTrace();
        }
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public void stop(BundleContext context) throws Exception {
    System.out.println("Cleaning up");
    Thread.sleep(1000);
    System.out.println("Goodbye World" + context.toString());
  }
}