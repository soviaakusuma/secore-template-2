/*
 * Copyright (c) 2010-2020 DGIT Systems Pty. Ltd. All Rights Reserved.
 *
 * This program and the accompanying materials are the property of DGIT
 * Systems Pty. Ltd.
 *
 * You may obtain a copy of the Licence at http://www.dgit.biz/licence
 */


package com.inomial.secore.template;

import com.inomial.secore.mon.MonitoringServer;
import com.telflow.assembly.healthcheck.Healthcheck;
import com.telflow.assembly.healthcheck.HealthcheckApi;
import com.telflow.assembly.healthcheck.HealthcheckService;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Application;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

/**
 * Publish a JSON HTTP API which exposes the health of the application.
 *
 * @author laurie
 */
public class HealthcheckServer {

    private static HealthcheckService service;

    private MonitoringServer server;

    public static class HealthcheckApplication extends Application {

        private final Set<Object> it = Collections.singleton(new HealthcheckApi(service));

        @Override
        public Set<Object> getSingletons() {
            return this.it;
        }
    }

    public synchronized void startServer(String appName, Map<String, Healthcheck> checks, long perCheckWait, int port) {
        if (service != null) {
            stopServer();
        }
        service = new HealthcheckService(appName, perCheckWait);
        service.setHealthchecks(checks);
        service.setDoChecks(checks.keySet().stream().collect(Collectors.joining(",")));

        this.server = new MonitoringServer(MonitoringServer.DEFAULT_PATH, port).addHotspotMetrics();
        ServletHolder holder = new ServletHolder("healthchecks", new HttpServletDispatcher());
        holder.setInitParameter("javax.ws.rs.Application", HealthcheckApplication.class.getName());
        this.server.publishMonitor(holder, "health");

        try {
            this.server.start();
        } catch (Exception e) {
            throw new RuntimeException("Unable to start healthcheck service", e);
        }
    }

    public synchronized void stopServer() {
        try {
            if (this.server != null) {
                this.server.stop();
            }
            this.server = null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to stop healthcheck service", e);
        }
        if (service != null) {
            service.destroy();
            service = null;
        }
    }
}
