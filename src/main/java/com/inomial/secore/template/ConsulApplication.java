
package com.inomial.secore.template;

import com.inomial.secore.health.Healthcheck;
import com.inomial.secore.health.kafka.KafkaHealthcheck;
import com.inomial.secore.health.postgres.PostgresHealthcheck;
import com.inomial.secore.kafka.KafkaHeaderMapper;
import com.inomial.secore.kafka.KafkaMessage;
import com.inomial.secore.kafka.MessageConsumer;
import com.inomial.secore.kafka.MessageHandler;
import com.inomial.secore.kafka.MessageProducer;
import com.inomial.secore.mon.MonitoringServer;
import com.inomial.secore.scope.Scope;
import com.telflow.factory.configuration.management.ConsulManager;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private MessageProducer mp;

    private MessageConsumer mc;

    public void start() throws Exception {
        startConsul();
    }

    private void consulChanged() {
        LOG.info("Consul changed, updating configuration.");
        logConsulKeys();

        Main.initialisedHealthCheck.starting();
        startHealthcheckServer();
        migrateDB();
        startKafka();

        //
        // → → → → → (Re)start your application here. ← ← ← ← ←
        //

        LOG.info("Configuration applied.");
        Main.initialisedHealthCheck.initialised();
    }

    private void logConsulKeys() {
        Predicate<String> isSecretKey = k ->
            k == null || k.toLowerCase().contains("secure") || k.toLowerCase().contains("pass");

        String keys = Arrays.stream(Consul.values())
            .map(entry -> entry.key() + " " + (isSecretKey.test(entry.key()) ? "*omitted*" : entry.value()))
            .sorted()
            .collect(Collectors.joining("\n"));
        LOG.info("Configuration:\n{}", keys);
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
            Consul.APP_POSTGRES_USER.value(),
            Consul.APP_POSTGRES_PASS.value()));
        checks.put("initialised", Main.initialisedHealthCheck); // this is marked as done in Main

        Long wait = Long.valueOf(Consul.HEALTHCHECK_WAIT.value());
        Integer port = Integer.valueOf(Consul.HEALTHCHECK_PORT.value());
        Main.health.startServer(CONSUL_APP_NAME, checks, wait, port);
    }

    private void startKafka() {
        if (this.mp != null) {
            this.mp.close();
        }
        if (this.mc != null) {
            this.mc.shutdown(true);
        }

        this.mp = new MessageProducer(createProducerConfig());
        this.mc = new MessageConsumer(createConsumerConfig());
        MessageHandler h = rec -> handle(rec.value());
        this.mc.addMessageHandler(Consul.KAFKA_CONSUME_TOPIC.value(), h, Scope.NONE);
        this.mc.start(Consul.KAFKA_CONSUME_GROUP.value());
    }

    private void handle(String value) {
        LOG.info("Received message: {}", value);
        //
        // → → → → → Process your incoming message here. ← ← ← ← ←
        //
    }

    protected void sendMessage(UUID correlationId, String message) {
        this.mp.send(new KafkaMessage(Consul.KAFKA_PRODUCE_TOPIC.value(), null, message) {

            @Override
            public ProducerRecord<Object, String> produce() {
                // KafkaMessage predefined headers are prepended with "inomial", so … do it here for now
                ProducerRecord<Object, String> pr = super.produce();
                KafkaHeaderMapper m = new KafkaHeaderMapper(pr.headers());

                m.setUuidHeader("correlation", correlationId);
                m.setUuidHeader("uuid", UUID.randomUUID());
                m.setStringHeader("messageVersion", "0");

                return pr;
            }
        });
    }

    /**
     * @return configuration for the kafka consumer
     */
    protected Map<String, Object> createConsumerConfig() {
        Map<String, Object> cp = new HashMap<>();
        cp.put(ConsumerConfig.GROUP_ID_CONFIG, createConsumerId());
        cp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Consul.ENV_KAFKA.value());
        cp.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return cp;
    }

    /**
     * @return competing consumer ID (shares same ID as all other instances of this application)
     */
    private String createConsumerId() {
        return CONSUL_APP_NAME;
    }

    /**
     * @return configuration for the kafka producer
     */
    protected Map<String, Object> createProducerConfig() {
        Map<String, Object> pp = new HashMap<>();
        pp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Consul.ENV_KAFKA.value());
        pp.put(ProducerConfig.CLIENT_ID_CONFIG, createProducerId());
        pp.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        pp.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return pp;
    }

    /**
     * @return unique identifier ID (assuming single instance per host)
     */
    private String createProducerId() {
        try {
            return CONSUL_APP_NAME + "_" + InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOG.warn("Unable to get hostname", e);
            return CONSUL_APP_NAME;
        }
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

    private void migrateDB() {
        DataSource ds = null;

        String url = String.format("jdbc:postgresql://%s:%s/%s",
            Consul.ENV_POSTGRES_HOST.value(),
            Consul.ENV_POSTGRES_PORT.value(),
            Consul.APP_POSTGRES_DB.value());
        String user = Consul.APP_POSTGRES_USER.value();
        String pwd = Consul.APP_POSTGRES_PASS.value();

        Flyway fw = Flyway.configure().
            dataSource(url, user, pwd).
            locations("classpath:schema").
            load();

        int result = fw.migrate();
        LOG.info("Applied {} migrations.", result);
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

        KAFKA_PRODUCE_TOPIC("/kafka/producer", String.format("solution.%s.outbox", CONSUL_APP_NAME), KeyType.App),
        KAFKA_CONSUME_TOPIC("/kafka/consumer", String.format("solution.%s.inbox", CONSUL_APP_NAME), KeyType.App),
        KAFKA_CONSUME_GROUP("/kafka/group", CONSUL_APP_NAME, KeyType.App),

        HEALTHCHECK_PORT("/healthcheck/port", Integer.toString(MonitoringServer.DEFAULT_PORT), KeyType.App),
        HEALTHCHECK_WAIT("/healthcheck/perCheckMaxWait", "150", KeyType.App),

        // Adjust to suit:
        APP_POSTGRES_DB("/postgres/database", "tf_myapp", KeyType.App),
        APP_POSTGRES_USER("/postgres/user", "postgres", KeyType.App),
        APP_POSTGRES_PASS("/postgres/secure/password", "", KeyType.App),

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
