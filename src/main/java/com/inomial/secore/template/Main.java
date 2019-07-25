package com.inomial.secore.template;

import com.inomial.secore.mon.MonitoringServer;

public class Main {
  public static void main(String argv[]) {
  
    // In general, you should leave this in. By default, secore provides instrumentation for the JVM.
    // You can also add your own metrics, see the secore README for links.
    MonitoringServer.Start();

    System.out.println(Main.class.getName() + " started. JVM stats are published on port 7070.");
    
    // Explicitly calling System.exit() is REALLY IMPORTANT to prevent processes
    // entering a live-lock due to daemon threads keeping a dead application running.
    // Don't remove this line unless you have a really good reason.
    System.exit(1);
  }
}
