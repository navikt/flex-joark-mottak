import config.KafkaConfig
import mock.DokarkivMockDispatcher
import mock.KodeverkMockDispatcher
import mock.OppgaveMockDispatcher
import mock.PdlMockDispatcher
import mock.SafMockDispatcher
import no.nav.helse.flex.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockWebServer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class, KafkaConfig::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@AutoConfigureObservability
abstract class FellesTestOppsett {
    companion object {
        val topic: String get() = System.getProperty("AIVEN_DOKUMENT_TOPIC")

        init {

            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).apply {
                start()
                System.setProperty("KAFKA_BROKERS", bootstrapServers)
                System.setProperty("AIVEN_DOKUMENT_TOPIC", "test-topic")
                System.setProperty("KAFKA_SCHEMA_REGISTRY", "mock://localhost.nav")
            }
        }

        val safMockWebserver =
            MockWebServer().apply {
                System.setProperty("SAF_URL", "http://localhost:$port")
                dispatcher = SafMockDispatcher
            }

        val dokarkivMockWebserver =
            MockWebServer().apply {
                System.setProperty("DOKARKIV_URL", "http://localhost:$port")
                dispatcher = DokarkivMockDispatcher
            }

        val pdlMockWebserver =
            MockWebServer().apply {
                System.setProperty("PDL_URL", "http://localhost:$port")
                dispatcher = PdlMockDispatcher
            }

        val oppgaveMockWebserver =
            MockWebServer().apply {
                System.setProperty("OPPGAVE_URL", "http://localhost:$port")
                dispatcher = OppgaveMockDispatcher
            }

        val kodeverkMockWebServer =
            MockWebServer().apply {
                System.setProperty("FKV_URL", "http://localhost:$port")
                dispatcher = KodeverkMockDispatcher
            }
    }

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, Any>
}
