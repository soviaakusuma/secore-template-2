
package com.inomial.secore.template;

import com.inomial.secore.health.HealthcheckServer;
import com.inomial.secore.health.InitialisedHealthCheck;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {

    static HealthcheckServer health;
    static InitialisedHealthCheck initialisedHealthCheck = new InitialisedHealthCheck();
    
    public static void main(String[] argv) {

        try {
            // Force libraries that use j.u.l. to use the slf4j handler:
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();
            LoggerFactory.getLogger(Main.class).info("Starting.");

            //
            // Note: health#startServer starts MonitoringServer.
            //
            health = new HealthcheckServer();
            //
            // → → → → → If your application does not use consul,    ← ← ← ← ←
            // → → → → → uncomment to use either option 1 or 2 below ← ← ← ← ←
            // → → → → → and adjust to suit:                         ← ← ← ← ←
            // → → → → → Option 1: when need to specify values to initialize
//            Map<String, Healthcheck> checks = new HashMap<>();
//            checks.put("kafka", new KafkaHealthcheck(KafkaProperties.getBootstrapServer(), 10000L));
//            String url = System.getenv("DS_URL");
//            String username = System.getenv("DS_USERNAME");
//            String password = System.getenv("DS_PASSWORD");
//            checks.put("postgresql", new PostgresHealthcheck(url, username, password));
//            checks.put("initialised", initialisedHealthCheck);
//            // … add other checks if you have some …
//            health.startServer(System.getenv("SERVICE") + "." + System.getenv("INSTANCE") + "." + System.getenv("TASK_SLOT"),
//                               checks,
//                               150L,
//                               MonitoringServer.DEFAULT_PORT);
            // → → → → → Option 2: when use default values to initialize
//            InitialisedHealthCheck initialisedHealthCheck = health.addInitialisedHealthCheck();
//            health.addPostgresHealthcheck();
//            health.addKafkaHealthcheck();
//            // … add other checks if you have some such as Kafka MessageConsumer/ManagedConsumer
//            health.addManagedConsumerHealthcheck(managedConsumer);
//            health.startDefaultServer();

            // → → → → → If your application does not use consul, ← ← ← ← ←
            // → → → → → delete the following line and start your ← ← ← ← ←
            // → → → → → application here.                        ← ← ← ← ←
            new ConsulApplication().start();

            
            // Done starting up
            initialisedHealthCheck.initialised();
            LoggerFactory.getLogger(Main.class).info("Started.");

            // For testing, park the main thread so the application stays up.
            // You can probably delete this in your application.
            if (argv.length > 0 && "wait".equals(argv[0])) {
                synchronized (Main.class) {
                    LoggerFactory.getLogger(Main.class).info("Waiting.");
                    Main.class.wait();
                }
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(Main.class).error("Failed to start application.", ex);
            System.exit(1);
        }
    }
}
