package no.nav.helse.flex.infrastructure.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Gauge
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import org.slf4j.LoggerFactory
import org.slf4j.MDC

object Metrics {
    private val log = LoggerFactory.getLogger(Metrics::class.java)
    private const val MANUELL = "Manuell journalforing"
    private const val AUTO = "Automatisk journalforing"

    private val jfrProcessCounter = Counter.Builder()
        .name("flex_joark_mottak_process_counter")
        .help("Teller antall journalposter som behandles. Setter på labels på om det burde blitt behandlet maskinelt/manuelt og om den blir behandlet maskinelt/manuelt")
        .labelNames("result", "desired", "tema", "kanal", "skjema")
        .register()

    private val feilregOppgaverCounter = Counter.Builder()
        .name("flex_joark_mottak_feilregistrerte_oppgaver_counter")
        .help("Teller antall ganger vi må avbryte autoatiske prosess på en slik måte at en oppgave må feilregistreres")
        .labelNames("ableToFeilreg", "tema", "kanal", "skjema")
        .register()

    private val functionalReqFailCounter = Counter.Builder()
        .name("flex_joark_mottak_functional_req_fail_counter")
        .help("Teller funksjonelle hindringer for automatisk journalforing")
        .labelNames("reason", "tema", "kanal", "skjema")
        .register()

    private val invalidJournalpostStatusCounter = Counter.Builder()
        .name("flex_joark_mottak_invalid_journalpost_status_counter")
        .help("Teller journalposter fra SAF med ugyldig status")
        .labelNames("status", "tema", "kanal", "skjema")
        .register()

    private val retryCounter = Counter.Builder()
        .name("flex_joark_mottak_retry_counter")
        .help("Teller antall journalposter hver gang vi vil prøve på nytt senere")
        .labelNames("transformer", "numErrors", "tema", "kanal", "skjema")
        .register()

    private val retrystoreGauge = Gauge.Builder()
        .name("flex_joark_mottak_retry_gauge")
        .help("Viser hvor mange JP som ligger på retrystore")
        .labelNames("transformer")
        .register()

    private val jfrManuellProcessCounter = Counter.build(
        "jfr_manuell_process_counter",
        "Teller antall manuelle journalposter som behandles."
    )
        .labelNames("result", "desired", "tema")
        .register()

    private val createOppgaveCounter = Counter.build(
        "jfr_manuell_create_oppgaver_counter",
        "Teller antall manuelle oppgaver som opprettes. Diffrensierer på fordeling og journalføringsoppgave"
    )
        .labelNames("oppgavetype", "tema")
        .register()

    fun incJfrManuallProcess(enrichedKafkaEvent: EnrichedKafkaEvent, desiredAutomaticJfr: Boolean) {
        val desired: String
        var skjema = "null"

        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)

            desired = if (desiredAutomaticJfr) AUTO else MANUELL

            if (enrichedKafkaEvent.journalpost != null && enrichedKafkaEvent.skjema != null) {
                skjema = enrichedKafkaEvent.skjema!!
            }

            jfrProcessCounter.labels(
                MANUELL,
                desired,
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                skjema
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incJfrManuallProcess", e)
        }
    }

    fun incJfrAutoProcess(enrichedKafkaEvent: EnrichedKafkaEvent) {
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
            jfrProcessCounter.labels(
                AUTO,
                AUTO,
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                enrichedKafkaEvent.skjema.toString()
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incJfrAutoProcess", e)
        }
    }

    fun incFeilregCounter(enrichedKafkaEvent: EnrichedKafkaEvent, sucessfullFeilreg: Boolean) {
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
            feilregOppgaverCounter.labels(
                java.lang.Boolean.toString(sucessfullFeilreg),
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                enrichedKafkaEvent.skjema.toString()
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incFeilregCounter", e)
        }
    }

    fun incFailFunctionalRequirements(reason: String?, enrichedKafkaEvent: EnrichedKafkaEvent) {
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
            functionalReqFailCounter.labels(
                reason,
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                enrichedKafkaEvent.skjema.toString()
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incFailFunctionalRequirements", e)
        }
    }

    fun incInvalidJournalpostStatus(enrichedKafkaEvent: EnrichedKafkaEvent) {
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
            invalidJournalpostStatusCounter.labels(
                enrichedKafkaEvent.journalpost?.journalstatus,
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                enrichedKafkaEvent.skjema.toString()
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incInvalidJournalpostStatus", e)
        }
    }

    fun incRetry(transformer: String?, enrichedKafkaEvent: EnrichedKafkaEvent) {
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
            retryCounter.labels(
                transformer,
                enrichedKafkaEvent.numFailedAttempts.toString(),
                enrichedKafkaEvent.tema,
                enrichedKafkaEvent.kafkaEvent.mottaksKanal,
                enrichedKafkaEvent.skjema.toString()
            ).inc()
        } catch (e: Exception) {
            log.error("Klarte ikke å inkrementere metrikk incRetry", e)
        }
    }

    fun setRetrystoreGauge(name: String?, num: Long) {
        try {
            retrystoreGauge.labels(name).set(num.toDouble())
        } catch (e: Exception) {
            log.error("Klarte ikke sette retry gauge")
        }
    }

    fun incProcessCounter(result: String?, desireResult: String?, tema: String?) {
        jfrManuellProcessCounter.labels(result, desireResult, tema).inc()
    }

    fun incCreateOppgaveCounter(oppgavetype: String?, tema: String?) {
        createOppgaveCounter.labels(oppgavetype, tema).inc()
    }
}
