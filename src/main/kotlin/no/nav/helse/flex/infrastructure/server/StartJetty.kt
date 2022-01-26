package no.nav.helse.flex.infrastructure.server

import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.flex.infrastructure.kafka.JfrGenerellKafkaService
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory

class StartJetty {
    private val jettyServer = Server(port)
    private val port get() = System.getProperty("jfr_arena.port", "8080").toInt()

    fun start() {
        val context = ServletContextHandler(jettyServer, "/")
        registerMetricsServlet(context)
        registerNaisServlets(context)
        configureHeaderSize()
        jettyServer.handler = context
        DefaultExports.initialize()
        jettyServer.start()
        log.info("Startet jetty")
    }

    fun stop() {
        jettyServer.stop()
    }

    private fun configureHeaderSize() {
        for (c in jettyServer.connectors) {
            c.getConnectionFactory(HttpConnectionFactory::class.java).httpConfiguration.requestHeaderSize = 16384
        }
    }

    private fun registerMetricsServlet(context: ServletContextHandler) {
        val metricsServlet = ServletHolder(MetricsServlet())
        context.addServlet(metricsServlet, "/internal/metrics/*")
    }

    private fun registerNaisServlets(context: ServletContextHandler) {
        val readyServlet = ServletHolder(ReadyCheckServlet())
        context.addServlet(readyServlet, "/internal/ready/*")
        val aliveServlet = ServletHolder(AliveCheckServlet())
        context.addServlet(aliveServlet, "/internal/alive/*")
        val prestopServlet = ServletHolder(PrestopServlet())
        context.addServlet(prestopServlet, "/internal/prestop/*")
    }

    companion object {
        private val log = LoggerFactory.getLogger(StartJetty::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val jetty = StartJetty()
            try {
                log.info("Starter server")
                jetty.start()
                val service = JfrGenerellKafkaService()
                service.start()
            } catch (e: Exception) {
                log.error("Kunne ikke starte opp server", e)
                jetty.stop()
            }
        }
    }
}
