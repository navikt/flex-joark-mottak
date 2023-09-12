import no.nav.helse.flex.Application
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import kotlin.concurrent.thread

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [Application::class])
@AutoConfigureObservability
abstract class BaseTestClass {
    companion object {
        init {
            val threads = mutableListOf<Thread>()

            thread {
                KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0")).apply {
                    start()
                    System.setProperty("KAFKA_BROKERS", bootstrapServers)
                }
            }.also { threads.add(it) }

            threads.forEach { it.join() }
        }
    }
}
