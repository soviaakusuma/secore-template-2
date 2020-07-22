
package com.inomial.secore.template;

import com.inomial.secore.mon.MonitoringServer;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {

    public static void main(String[] argv) {

        try {
            // Force libraries that use j.u.l. to use the slf4j handler:
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            LoggerFactory.getLogger(Main.class).info("Starting. JVM stats are published on port 7070.");

            // In general, you should leave this in. By default, secore provides instrumentation for the JVM.
            // You can also add your own metrics, see the secore README for links.
            MonitoringServer.Start();

            // → → → → → Start your application here. ← ← ← ← ←

            LoggerFactory.getLogger(Main.class).info("Started.");
        } catch (Exception ex) {
            LoggerFactory.getLogger(Main.class).error("Failed to start application.", ex);
            System.exit(1);
        }
    }
}
