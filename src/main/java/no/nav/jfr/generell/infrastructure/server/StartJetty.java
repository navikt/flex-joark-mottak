package no.nav.jfr.generell.infrastructure.server;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import no.nav.jfr.generell.infrastructure.kafka.JfrGenerellKafkaService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartJetty {
    private static final Logger log = LoggerFactory.getLogger(StartJetty.class);
    private final Server jettyServer = new Server(getPort());

    public static void main(final String[] args) throws Exception {
        StartJetty jetty = new StartJetty();
        try {
            log.info("Starter server");
            jetty.start();
            final JfrGenerellKafkaService service = new JfrGenerellKafkaService();
            service.start();
        }catch (Exception e){
            log.error("Kunne ikke starte opp server", e);
            jetty.stop();
        }
    }

    void start() throws Exception {
        final ServletContextHandler context = new ServletContextHandler(jettyServer, "/");
        registerMetricsServlet(context);
        registerNaisServlets(context);
        configureHeaderSize();
        jettyServer.setHandler(context);
        DefaultExports.initialize();
        jettyServer.start();
        log.info("Startet jetty");
    }

    void stop() throws Exception {
        jettyServer.stop();
    }

    private void configureHeaderSize() {
        for (final Connector c : jettyServer.getConnectors()) {
            c.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setRequestHeaderSize(16384);
        }
    }

    private void registerMetricsServlet(final ServletContextHandler context) {
        final ServletHolder metricsServlet = new ServletHolder(new MetricsServlet());
        context.addServlet(metricsServlet, "/internal/metrics/*");
    }

    private void registerNaisServlets(final ServletContextHandler context) {
        final ServletHolder readyServlet = new ServletHolder(new ReadyCheckServlet());
        context.addServlet(readyServlet, "/internal/ready/*");

        final ServletHolder aliveServlet = new ServletHolder(new AliveCheckServlet());
        context.addServlet(aliveServlet, "/internal/alive/*");

        final ServletHolder prestopServlet = new ServletHolder(new PrestopServlet());
        context.addServlet(prestopServlet, "/internal/prestop/*");
    }

    private int getPort() {
        return Integer.parseInt(System.getProperty("jfr_arena.port", "8080"));
    }
}
