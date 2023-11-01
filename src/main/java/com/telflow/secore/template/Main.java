/*
 * Copyright (c) 2010-2022 DGIT Systems Pty. Ltd. All Rights Reserved.
 *
 * This program and the accompanying materials are the property of DGIT
 * Systems Pty. Ltd.
 *
 * You may obtain a copy of the Licence at http://www.dgit.biz/licence
 */

package com.telflow.secore.template;

import com.inomial.secore.health.HealthcheckServer;
import com.inomial.secore.health.InitialisedHealthCheck;

import java.util.concurrent.CountDownLatch;

import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class Main {

    static final InitialisedHealthCheck INITIALISED = new InitialisedHealthCheck();

    private static HealthcheckServer health;

    private Main() {

    }

    /**
     * start the application
     *
     * @param argv params
     */
    public static void main(String[] argv) {

        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            LoggerFactory.getLogger(Main.class).info("Starting.");

            health = new HealthcheckServer();
            new ConsulApplication().start();

            INITIALISED.initialised();
            LoggerFactory.getLogger(Main.class).info("Started.");

            if (argv.length > 0 && "wait".equals(argv[0])) {
                LoggerFactory.getLogger(Main.class).info("Waiting.");
                new CountDownLatch(1).await();
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(Main.class).error("Failed to start application.", ex);
            System.exit(1);
        }
    }

    public static HealthcheckServer getHealthcheckServer() {
        return health;
    }
}
