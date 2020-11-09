
package com.inomial.secore.template;

import com.inomial.secore.mon.MonitoringServer;
import com.telflow.assembly.healthcheck.Healthcheck;
import com.telflow.assembly.healthcheck.kafka.KafkaHealthcheck;
import com.telflow.assembly.healthcheck.postgres.PostgresHealthcheck;
import com.telflow.factory.configuration.management.ConsulManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consume configuration and updates from Consul.
 */
public class ConsulApplication {

    /**
     * This application's namespace for consul configuration.
     * Use a name as per <a href=https://dgit.atlassian.net/wiki/spaces/DPE/pages/988123028/Consul+KV+Hierarchy">Consul
     * doco</a>
     */
    static final String CONSUL_APP_NAME = "consul-main";

    private static final Logger LOG = LoggerFactory.getLogger(ConsulApplication.class);

    public void start() throws Exception {
        startConsul();
    }

    private void consulChanged() {
        LOG.info("Configuration: {}", ConsulManager.getConfiguration());
        startHealthcheckServer();

        //
        // → → → → → (Re)start your application here. ← ← ← ← ←
        //

        LOG.info("Configuration applied.");
    }

    private void startHealthcheckServer() {
        Map<String, Healthcheck> checks = new HashMap<>();
        checks.put("kafka", new KafkaHealthcheck(Consul.ENV_KAFKA.value(), 10000L));
        String url = String.format("jdbc:postgresql://%s:%s/%s",
            Consul.ENV_POSTGRES_HOST.value(),
            Consul.ENV_POSTGRES_PORT.value(),
            Consul.APP_POSTGRES_DB.value());
        checks.put("postgresql", new PostgresHealthcheck(
            url,
            Consul.ENV_POSTGRES_USER.value(),
            Consul.ENV_POSTGRES_PASS.value()));

        Long wait = Long.valueOf(Consul.HEALTHCHECK_WAIT.value());
        Integer port = Integer.valueOf(Consul.HEALTHCHECK_PORT.value());
        Main.health.startServer(checks, wait, port);
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
        //
        // We have to set app name before we build defaults, because building the keys for the
        // defaults requires the app name
        //
        ConsulManager.setAppName(CONSUL_APP_NAME);
        ConsulManager.init(url, CONSUL_APP_NAME, Consul.defaults());
        // If we register the method before init, it's called twice with partial config the first time.
        // If we register the method after init, it's called only after consul config next changes.
        // So register after and call explicitly.
        ConsulManager.addRegisteredObject(ConsulApplication.class.getName(), this::consulChanged);
        consulChanged();
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

        private KeyType(Function<String, String> map, Function<String, String> get) {
            this.toKey = map;
            this.getValue = get;
        }
    }

    public enum Consul {

        //
        // → → → → → Replace these enum values with your application config. ← ← ← ← ←
        //
        APP_CFG_A("/appConfig1", "defaultValue1", KeyType.App),
        APP_CFG_B("/appConfig2", "defaultValue2", KeyType.App),

        APP_POSTGRES_DB("/postgres/database", "tf_system_message", KeyType.App),

        HEALTHCHECK_PORT("/healthcheck/port", Integer.toString(MonitoringServer.DEFAULT_PORT), KeyType.App),
        HEALTHCHECK_WAIT("/healthcheck/perCheckMaxWait", "150", KeyType.App),

        // Adjust to suit:
        ENV_POSTGRES_USER("/postgres/postgresSystemMessageUser", "tf_system_message", KeyType.Env),
        ENV_POSTGRES_PASS("/postgres/secure/postgresSystemMessagePassword", "", KeyType.Env),

        ENV_POSTGRES_HOST("/postgres/postgresEndpoint", "postgresql", KeyType.Env),
        ENV_POSTGRES_PORT("/postgres/postgresPort", "5432", KeyType.Env),
        ENV_KAFKA("/kafka/kafkaEndpoint", "kafka:9092", KeyType.Env);

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
    }
}
