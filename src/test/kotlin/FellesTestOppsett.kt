import config.KafkaConfig
import mock.DokarkivMockDispatcher
import mock.KodeverkMockDispatcher
import mock.OppgaveMockDispatcher
import mock.PdlMockDispatcher
import mock.SafMockDispatcher
import mockwebserver3.MockWebServer
import no.nav.helse.flex.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class, KafkaConfig::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@AutoConfigureMetrics
@AutoConfigureTracing
abstract class FellesTestOppsett {
    companion object {
        val topic: String get() = System.getProperty("AIVEN_DOKUMENT_TOPIC")

        init {

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.9.1")).apply {
                start()
                System.setProperty("KAFKA_BROKERS", bootstrapServers)
                System.setProperty("AIVEN_DOKUMENT_TOPIC", "test-topic")
                System.setProperty("KAFKA_SCHEMA_REGISTRY", "mock://localhost.nav")
            }
        }

        val safMockWebserver =
            MockWebServer().apply {
                dispatcher = SafMockDispatcher
                start()
                System.setProperty("SAF_URL", "http://localhost:$port")
            }

        val dokarkivMockWebserver =
            MockWebServer().apply {
                dispatcher = DokarkivMockDispatcher
                start()
                System.setProperty("DOKARKIV_URL", "http://localhost:$port")
            }

        val pdlMockWebserver =
            MockWebServer().apply {
                dispatcher = PdlMockDispatcher
                start()
                System.setProperty("PDL_URL", "http://localhost:$port")
            }

        val oppgaveMockWebserver =
            MockWebServer().apply {
                dispatcher = OppgaveMockDispatcher
                start()
                System.setProperty("OPPGAVE_URL", "http://localhost:$port")
            }

        val kodeverkMockWebServer =
            MockWebServer().apply {
                dispatcher = KodeverkMockDispatcher
                start()
                System.setProperty("KODEVERK_URL", "http://localhost:$port")
            }
    }

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, Any>
}
