/*
 * Copyright (c) 2010-2022 DGIT Systems Pty. Ltd. All Rights Reserved.
 *
 * This program and the accompanying materials are the property of DGIT
 * Systems Pty. Ltd.
 *
 * You may obtain a copy of the Licence at http://www.dgit.biz/licence
 */

package com.telflow.secore.template;

import com.inomial.secore.http.HttpServer;
import com.telflow.factory.configuration.management.ConsulManager;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consume configuration and updates from Consul.
 */
public class ConsulApplication {

    /**
     * This application's namespace for consul configuration.
     * Use a name as per
     * <a href="https://dgit.atlassian.net/wiki/spaces/DPE/pages/988123028/Consul+KV+Hierarchy">Consul doco</a>
     */
    static final String CONSUL_APP_NAME = "myapp";

    private static final Logger LOG = LoggerFactory.getLogger(ConsulApplication.class);

    public void start() throws IOException {
        startConsul();
    }

    private void testApi() {
        HttpServer.addResourceClass(TestApiClass.class);
        HttpServer.start(Collections.emptySet(), Collections.emptySet());
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    private void consulChanged() {
        try {
            LOG.info("Consul changed, updating configuration.");
            logConsulKeys();
            Main.INITIALISED.starting();
            LOG.info("Configuration applied.");
            Main.INITIALISED.initialised();
        } catch (ThreadDeath ok) {
            throw ok;
        } catch (Throwable ohno) {
            LOG.error("Unable to apply configuration, exiting.", ohno);
            System.exit(2);
        }
    }

    private static void logConsulKeys() {
        Predicate<String> isSecretKey = k ->
                k == null || k.toLowerCase().contains("secure") || k.toLowerCase().contains("pass");

        String keys = Arrays.stream(Consul.values())
                .map(entry -> entry.key() + " " + (isSecretKey.test(entry.key()) ? "*omitted*" : entry.value()))
                .sorted()
                .collect(Collectors.joining("\n"));
        LOG.info("Configuration:\n{}", keys);
    }

    /**
     * @throws MalformedURLException if the consul server environment variables cannot be parsed
     */
    private void startConsul() throws MalformedURLException {
        String consulEndpoint = System.getenv("CONSUL_SERVER");
        if (consulEndpoint == null) {
            LOG.error("Consul not configured. Set environment variable \"CONSUL_SERVER\" or do not use consul.");
            return;
        }
        LOG.info("Consul Endpoint: {}", consulEndpoint);
        URL url = new URL(consulEndpoint);
        ConsulManager.setAppName(CONSUL_APP_NAME);
        ConsulManager.init(url, CONSUL_APP_NAME, Consul.defaults());
        ConsulManager.addRegisteredObject(ConsulApplication.class.getName(), this::consulChanged);
        consulChanged();
        testApi();
    }
}
