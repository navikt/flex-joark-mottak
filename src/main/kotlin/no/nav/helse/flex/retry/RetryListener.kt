package no.nav.helse.flex.retry

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.*
import no.nav.helse.flex.journalpost.JournalpostBehandler
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*
import kotlin.math.min

const val RETRY_TOPIC = "flex." + "flex-joark-mottak-retry"
const val BEHANDLINGSTIDSPUNKT = "behandlingstidspunkt"

@Component
class RetryListener(
    private val journalpostBehandler: JournalpostBehandler,
    private val retryProducer: RetryProducer
) {
    val log = logger()

    @KafkaListener(
        topics = [RETRY_TOPIC],
        id = "flex-joark-mottak-retry",
        idIsGroup = true,
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = [ "auto.offset.reset=earliest" ]
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val kafkaEvent = objectMapper.readValue<KafkaEvent>(cr.value())
        val behandlingstidspunkt = cr.headers().lastHeader(BEHANDLINGSTIDSPUNKT)
            ?.value()
            ?.let { String(it, StandardCharsets.UTF_8) }
            ?.let { Instant.ofEpochMilli(it.toLong()) }
            ?: Instant.now()

        try {
            val sovetid = behandlingstidspunkt.sovetid()
            if (sovetid > 0) {
                log.info("Mottok rebehandling av journalpost ${kafkaEvent.journalpostId} med behandlingstidspunkt ${behandlingstidspunkt.tilOsloLocalDateTime()} sover i $sovetid millisekunder")
                acknowledgment.nack(Duration.ofMillis(sovetid))
            } else {
                MDC.put(CORRELATION_ID, UUID.randomUUID().toString())
                journalpostBehandler.behandleJournalpost(kafkaEvent)
                acknowledgment.acknowledge()
            }
        } catch (e: Exception) {
            log.error("Rebehandling feilet for journalpost ${kafkaEvent.journalpostId}, legger tilbake på retry-topic", e)
            retryProducer.send(kafkaEvent, OffsetDateTime.now().plusMinutes(10))
            acknowledgment.acknowledge()
        } finally {
            MDC.clear()
        }
    }

    private fun Instant.sovetid(): Long {
        val sovetid = this.toEpochMilli() - Instant.now().toEpochMilli()
        val maxSovetid = 1000L * 60 * 1 // Skjer en rebalansering hvis den sover i mer enn 5 min
        return min(sovetid, maxSovetid)
    }
}
