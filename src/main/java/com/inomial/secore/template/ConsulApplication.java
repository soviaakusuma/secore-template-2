
package com.inomial.secore.template;

import com.inomial.secore.health.Healthcheck;
import com.inomial.secore.health.kafka.KafkaHealthcheck;
import com.inomial.secore.health.postgres.PostgresHealthcheck;
import com.inomial.secore.kafka.KafkaHeaderMapper;
import com.inomial.secore.kafka.KafkaMessage;
import com.inomial.secore.kafka.MessageConsumer;
import com.inomial.secore.kafka.MessageHandler;
import com.inomial.secore.kafka.MessageProducer;
import com.inomial.secore.scope.Scope;
import com.telflow.factory.configuration.management.ConsulManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
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

    private static final String DB_URL = "jdbc:postgresql://%s:%s/%s";

    private static final long HEALTHCHECK_WAIT_INTERVAL = 10000L;

    private String kafkaEndpoint;

    private MessageProducer mp;

    private MessageConsumer mc;

    public void start() throws IOException {
        startConsul();
    }

    @SuppressWarnings("checkstyle:illegalcatch")
    private void consulChanged() {
        try {
            LOG.info("Consul changed, updating configuration.");
            logConsulKeys();
            this.kafkaEndpoint = String.format("%s:%s", Consul.ENV_KAFKA_HOST, Consul.ENV_KAFKA_PORT);
            Main.INITIALISED.starting();
            startHealthcheckServer();
            migrateDB();
            startKafka();

            //
            // → → → → → (Re)start your application here. ← ← ← ← ←
            //

            LOG.info("Configuration applied.");
            Main.INITIALISED.initialised();
        } catch (ThreadDeath ok) {
            //
            // Semantics of ThreadDeath require this to propagate:
            //
            throw ok;
        } catch (Throwable ohno) {
            //
            // We shouldn't attempt to recover or ignore anything else:
            //
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

    private void startHealthcheckServer() {
        Map<String, Healthcheck> checks = new HashMap<>();
        checks.put("kafka", new KafkaHealthcheck(this.kafkaEndpoint, HEALTHCHECK_WAIT_INTERVAL));
        String url = String.format(DB_URL,
            Consul.ENV_POSTGRES_HOST.value(),
            Consul.ENV_POSTGRES_PORT.value(),
            Consul.APP_POSTGRES_DB.value());
        checks.put("postgresql", new PostgresHealthcheck(
            url,
            Consul.APP_POSTGRES_USER.value(),
            Consul.APP_POSTGRES_PASS.value()));
        checks.put("initialised", Main.INITIALISED); // this is marked as done in Main

        Long wait = Long.valueOf(Consul.HEALTHCHECK_WAIT.value());
        Integer port = Integer.valueOf(Consul.HEALTHCHECK_PORT.value());
        Main.getHealthcheckServer().startServer(CONSUL_APP_NAME, checks, wait, port);
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
        cp.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaEndpoint);
        cp.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return cp;
    }

    /**
     * @return competing consumer ID (shares same ID as all other instances of this application)
     */
    private static String createConsumerId() {
        return CONSUL_APP_NAME;
    }

    /**
     * @return configuration for the kafka producer
     */
    protected Map<String, Object> createProducerConfig() {
        Map<String, Object> pp = new HashMap<>();
        pp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaEndpoint);
        pp.put(ProducerConfig.CLIENT_ID_CONFIG, createProducerId());
        pp.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        pp.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return pp;
    }

    /**
     * @return unique identifier ID (assuming single instance per host)
     */
    private static String createProducerId() {
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

    private static void migrateDB() {
        String url = String.format(DB_URL,
            Consul.ENV_POSTGRES_HOST.value(),
            Consul.ENV_POSTGRES_PORT.value(),
            Consul.APP_POSTGRES_DB.value());
        String user = Consul.APP_POSTGRES_USER.value();
        String pwd = Consul.APP_POSTGRES_PASS.value();

        Flyway fw = Flyway.configure().
            dataSource(url, user, pwd).
            locations("classpath:schema").
            load();

        MigrateResult result = fw.migrate();
        LOG.info("Applied {} migrations.", result.migrations.size());
    }
}
