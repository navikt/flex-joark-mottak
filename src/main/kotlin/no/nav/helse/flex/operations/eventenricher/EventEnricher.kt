package no.nav.helse.flex.operations.eventenricher

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incInvalidJournalpostStatus
import no.nav.helse.flex.operations.SkjemaMetadata
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient
import no.nav.helse.flex.operations.eventenricher.saf.SafClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EventEnricher(
    private val safClient: SafClient = SafClient(),
    private val pdlClient: PdlClient = PdlClient(),
    private val fkvClient: FkvClient = FkvClient(),
    private val skjemaMetadata: SkjemaMetadata = SkjemaMetadata()
) {
    private val log = LoggerFactory.getLogger(EventEnricher::class.java)
    private val KRUT_KODER_REFRESH_INTERVAL_HOURS = 5L

    private var fkvKrutkoder: FkvKrutkoder

    init {
        fkvKrutkoder = fkvClient.fetchKrutKoder()
        setupRefreshFellesKodeverkTimer()
    }

    fun createEnrichedKafkaEvent(enrichedKafkaEvent: EnrichedKafkaEvent) {
        enrichJournalpostInKafkaEvent(enrichedKafkaEvent)
        updateBehandlingValues(enrichedKafkaEvent)
        if (enrichedKafkaEvent.isPersonbruker) {
            enrichIdenterFromPDL(enrichedKafkaEvent)
        } else {
            log.info("Kaller ikke PDL da bruker på journalpost ${enrichedKafkaEvent.journalpostId} er ikke en person")
        }
    }

    private fun enrichJournalpostInKafkaEvent(enrichedKafkaEvent: EnrichedKafkaEvent) {
        val journalpost = retriveJournalpostFromSaf(enrichedKafkaEvent.kafkaEvent)
        enrichedKafkaEvent.journalpost = journalpost
        if (journalpost!!.invalidJournalpostStatus()) {
            log.info("Avslutter videre behandling da journalpost ${journalpost.journalpostId} har status ${journalpost.journalstatus}")
            incInvalidJournalpostStatus(enrichedKafkaEvent)
            throw InvalidJournalpostStatusException()
        }
        if (skjemaMetadata.isIgnoreskjema(enrichedKafkaEvent.tema, enrichedKafkaEvent.skjema)) {
            log.info("Avslutter videre behandling da journalpost ${enrichedKafkaEvent.journalpostId} har skjema ${enrichedKafkaEvent.skjema} på tema ${enrichedKafkaEvent.tema} som eksplisitt skal ignoreres!")
            throw InvalidJournalpostStatusException()
        }
        enrichedKafkaEvent.journalpost = journalpost
    }

    private fun retriveJournalpostFromSaf(kafkaEvent: KafkaEvent): Journalpost? {
        return try {
            val journalpost = safClient.retriveJournalpost(kafkaEvent.getJournalpostId())
            log.info("Hentet $journalpost fra Saf")
            journalpost
        } catch (se: ExternalServiceException) {
            log.error("Kunne ikke hente journalpost: ${kafkaEvent.getJournalpostId()} fra Saf")
            throw se
        }
    }

    private fun enrichIdenterFromPDL(enrichedKafkaEvent: EnrichedKafkaEvent) {
        val journalpost = enrichedKafkaEvent.journalpost!!
        if (journalpost.bruker == null) {
            log.info("Bruker er ikke satt på journalpost: ${journalpost.journalpostId}. Kan ikke hente fra PDL")
            throw FunctionalRequirementException()
        }

        val identer = pdlClient.retrieveIdenterFromPDL(
            journalpost.bruker!!.id,
            journalpost.tema,
            enrichedKafkaEvent.journalpostId
        )
        enrichedKafkaEvent.identer = identer
        log.info("Hentet alle id-nummer for bruker på journalpost: ${journalpost.journalpostId} fra PDL")
    }

    private fun updateBehandlingValues(enrichedKafkaEvent: EnrichedKafkaEvent) {
        val journalpost = enrichedKafkaEvent.journalpost!!

        try {
            var behandlingsTema = fkvKrutkoder.getBehandlingstema(journalpost.tema, journalpost.brevkode)
            val behandlingsType = fkvKrutkoder.getBehandlingstype(journalpost.tema, journalpost.brevkode)
            if (!(journalpost.behandlingstema == null || journalpost.behandlingstema!!.isEmpty())) {
                behandlingsTema = journalpost.behandlingstema
            }
            journalpost.behandlingstema = behandlingsTema
            journalpost.behandlingstype = behandlingsType
            log.info("Satt følgende verdier behandlingstema: '$behandlingsTema' og behandlingstype: '$behandlingsType' på journalpost ${journalpost.journalpostId}")
        } catch (e: Exception) {
            enrichedKafkaEvent.toFordeling = true
            enrichedKafkaEvent.isToManuell = true
            log.warn(
                "Klarte ikke finne behandlingsverdier for journalpost ${journalpost.journalpostId} med tema ${journalpost.tema} og skjema ${journalpost.brevkode}",
                e
            )
        }
    }

    private fun setupRefreshFellesKodeverkTimer() {
        val fellesKodeverkRefresher: TimerTask = object : TimerTask() {
            override fun run() {
                try {
                    fkvKrutkoder = fkvClient.fetchKrutKoder()
                } catch (e: Exception) {
                    log.error("Kunne ikke oppdatere KrutKoder fra FKV. Bruker gamle koder og prøver på nytt senere")
                }
            }
        }
        val executor = Executors.newSingleThreadScheduledExecutor()
        val refresherDelay = KRUT_KODER_REFRESH_INTERVAL_HOURS
        executor.scheduleAtFixedRate(fellesKodeverkRefresher, refresherDelay, refresherDelay, TimeUnit.HOURS)
    }
}
