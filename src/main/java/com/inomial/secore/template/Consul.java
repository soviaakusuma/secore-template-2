/*
 * Copyright (c) 2010-2022 DGIT Consultants Pty. Ltd. All Rights Reserved.
 *
 * This program and the accompanying materials are the property of DGIT
 * Consultants Pty. Ltd.
 *
 * You may obtain a copy of the License at http://www.dgit.biz/license
 */

package com.inomial.secore.template;

import com.inomial.secore.mon.MonitoringServer;
import com.telflow.factory.configuration.management.ConsulManager;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration data obtained from consul
 */
@SuppressWarnings("checkstyle:javadocvariable")
public enum Consul {

    //
    // → → → → → Replace these enum values with your application config. ← ← ← ← ←
    //
    APP_CFG_A("/appConfig1", "defaultValue1", KeyType.App),
    APP_CFG_B("/appConfig2", "defaultValue2", KeyType.App),

    KAFKA_PRODUCE_TOPIC("/kafka/producer",
        String.format("solution.%s.outbox", ConsulApplication.CONSUL_APP_NAME),
        KeyType.App),
    KAFKA_CONSUME_TOPIC("/kafka/consumer",
        String.format("solution.%s.inbox", ConsulApplication.CONSUL_APP_NAME),
        KeyType.App),
    KAFKA_CONSUME_GROUP("/kafka/group",
        ConsulApplication.CONSUL_APP_NAME,
        KeyType.App),

    HEALTHCHECK_PORT("/healthcheck/port", Integer.toString(MonitoringServer.DEFAULT_PORT), KeyType.App),
    HEALTHCHECK_WAIT("/healthcheck/perCheckMaxWait", "150", KeyType.App),

    // Adjust to suit:
    APP_POSTGRES_DB("/postgres/database", "tf_myapp", KeyType.App),
    APP_POSTGRES_USER("/postgres/user", "postgres", KeyType.App),
    APP_POSTGRES_PASS("/postgres/secure/password", "", KeyType.App),

    ENV_POSTGRES_PROTOCOL("/postgres/protocol", "", KeyType.Env),
    ENV_POSTGRES_HOST("/postgres/host", "postgresql", KeyType.Env),
    ENV_POSTGRES_PORT("/postgres/port", "5432", KeyType.Env),

    ENV_KAFKA_PROTOCOL("/kafka/protocol", "", KeyType.Env),
    ENV_KAFKA_HOST("kafka/host", "kafka", KeyType.Env),
    ENV_KAFKA_PORT("kafka/port", "9092", KeyType.Env);

    public final String key;

    private final String dflt;

    /**
     * The mapper function relies on static state in ConsulManager that is resolved later than static initialiser
     * time, so we must invoke it later rather than in the ctor here.
     */
    private final KeyType kt;

    Consul(String key, String dflt, KeyType map) {
        this.key = key;
        this.dflt = dflt;
        this.kt = map;
    }

    public static Map<String, String> defaults() {
        return Stream.of(Consul.values()).collect(Collectors.toMap(Consul::key, x -> x.dflt));
    }

    public String value() {
        return this.kt.getValue.apply(this.key);
    }

    public String key() {
        return this.kt.toKey.apply(this.key);
    }

    /**
     * Telflow Consul configuration distinguishes between application and environment
     * namespaces. This is reflected in {@link ConsulManager} API. This enum maps an
     * unadorned key into its namespace.
     */
    private enum KeyType {

        App(ConsulManager::buildAppKey, ConsulManager::getAppKey),
        Env(ConsulManager::buildEnvKey, ConsulManager::getEnvKey);

        private final Function<String, String> toKey;

        private final Function<String, String> getValue;

        KeyType(Function<String, String> map, Function<String, String> get) {
            this.toKey = map;
            this.getValue = get;
        }
    }
}
