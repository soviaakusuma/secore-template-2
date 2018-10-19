package com.inomial.template;

import com.inomial.secore.mon.MonitoringServer;

public class Main {
  public static void main(String argv[]) {
  
    // In general, you should leave this in. By default, secore provides instrumentation for the JVM.
    // You can also add your own metrics, see the secore README for links.
    MonitoringServer.Start();

    System.out.println(Main.class.getName() + " started. JVM stats are published on port 7070.");
  }
}
