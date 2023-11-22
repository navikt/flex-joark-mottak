package no.nav.helse.flex

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.journalpost.JournalpostBehandler
import no.nav.helse.flex.retry.RetryProducer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.*

@Component
class DokumentConsumer(
    private val journalpostBehandler: JournalpostBehandler,
    private val retryProducer: RetryProducer
) {
    private val log = logger()

    @KafkaListener(
        topics = ["#{environmentToggles.dokumentTopic()}"],
        id = "flex-joark-mottak",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"]
    )
    fun listen(cr: ConsumerRecord<String, GenericRecord>, acknowledgment: Acknowledgment) {
        val genericRecord = cr.value()

        if (genericRecord["temaNytt"].toString() != "SYK") {
            return
        }

        if (genericRecord["hendelsesType"].toString() !in listOf("MidlertidigJournalført", "Mottatt", "JournalpostMottatt")) {
            return
        }

        val kafkaEvent = objectMapper.readValue<KafkaEvent>(genericRecord.toString())

        try {
            MDC.put(CORRELATION_ID, UUID.randomUUID().toString())
            journalpostBehandler.behandleJournalpost(kafkaEvent)
        } catch (e: Exception) {
            log.error("Feilet på journalpost ${kafkaEvent.journalpostId}, legger på retry-topic", e)
            retryProducer.send(kafkaEvent, OffsetDateTime.now().plusSeconds(1))
        } finally {
            MDC.clear()
        }

        acknowledgment.acknowledge()
    }
}

data class KafkaEvent(
    val hendelsesId: String,
    val hendelsesType: String,
    val journalpostId: String,
    val temaNytt: String,
    val mottaksKanal: String,
    val journalpostStatus: String,
    val versjon: Int = 0,
    val temaGammelt: String,
    val kanalReferanseId: String,
    val behandlingstema: String = ""
)
