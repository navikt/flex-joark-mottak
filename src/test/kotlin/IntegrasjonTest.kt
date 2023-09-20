import no.nav.helse.flex.KafkaEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import util.fromKClassToGenericRecord
import java.util.concurrent.TimeUnit

class IntegrasjonTest : BaseTestClass() {
    @Test
    fun `Mottar sykmelding del D og oppretter oppgave for den`() {
        val a = KafkaEvent(
            hendelsesId = "123",
            versjon = 1,
            hendelsesType = "Mottatt",
            journalpostId = "123",
            journalpostStatus = "MOTTATT",
            temaGammelt = "SYK",
            temaNytt = "SYK",
            mottaksKanal = "JA",
            kanalReferanseId = "JA",
            behandlingstema = "NEI"
        )

        repeat(1) {
            kafkaProducer.send(
                ProducerRecord(
                    topic,
                    fromKClassToGenericRecord(a)
                )
            ).get()
        }

        Awaitility.await().atMost(2, TimeUnit.SECONDS).until {
            dokarkivMockWebserver.requestCount == 2
        }
    }
}
