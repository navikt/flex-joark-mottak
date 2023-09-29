import config.KafkaConfig
import mock.*
import no.nav.helse.flex.Application
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockWebServer
import org.apache.kafka.clients.producer.KafkaProducer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class, KafkaConfig::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@AutoConfigureObservability
abstract class BaseTestClass {
    companion object {
        val safMockWebserver: MockWebServer
        val dokarkivMockWebserver: MockWebServer
        val pdlMockWebserver: MockWebServer
        val oppgaveMockWebserver: MockWebServer
        val kodeverkMockWebServer: MockWebServer
        val topic: String get() = System.getProperty("AIVEN_DOKUMENT_TOPIC")

        init {
            val threads = mutableListOf<Thread>()

            thread {
                KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                    System.setProperty("AIVEN_DOKUMENT_TOPIC", "test-topic")
                    System.setProperty("KAFKA_SCHEMA_REGISTRY", "mock://localhost.nav")
                }
            }.also { threads.add(it) }

            safMockWebserver = MockWebServer().apply {
                System.setProperty("SAF_URL", "http://localhost:$port")
                dispatcher = SafMockDispatcher
            }

            dokarkivMockWebserver = MockWebServer().apply {
                System.setProperty("DOKARKIV_URL", "http://localhost:$port")
                dispatcher = DokarkivMockDispatcher
            }

            pdlMockWebserver = MockWebServer().apply {
                System.setProperty("PDL_URL", "http://localhost:$port")
                dispatcher = PdlMockDispatcher
            }

            oppgaveMockWebserver = MockWebServer().apply {
                System.setProperty("OPPGAVE_URL", "http://localhost:$port")
                dispatcher = OppgaveMockDispatcher
            }

            kodeverkMockWebServer = MockWebServer().apply {
                System.setProperty("FKV_URL", "http://localhost:$port")
                dispatcher = KodeverkMockDispatcher
            }

            threads.forEach { it.join() }
        }
    }

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, Any>
}
