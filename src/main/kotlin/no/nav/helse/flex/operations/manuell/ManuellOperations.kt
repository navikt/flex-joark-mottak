package no.nav.helse.flex.operations.manuell

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incProcessCounter
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.slf4j.LoggerFactory

class ManuellOperations(
    private val oppgaveClient: OppgaveClient = OppgaveClient()
) {
    fun doStuff(enrichedKafkaEvent: EnrichedKafkaEvent) {
        if (oppgaveClient.checkIfJournapostHasOppgave(enrichedKafkaEvent.journalpostId)) {
            log.info("Det finnes oppgave på journalpost ${enrichedKafkaEvent.journalpost}, avslutt videre behandling")
            return
        }
        val oppgave: Oppgave = createOppgave(enrichedKafkaEvent)
        log.info("Opprettet ${oppgave.oppgavetype}-oppgave: ${oppgave.id} på enhet ${oppgave.tildeltEnhetsnr} for journalpost: ${enrichedKafkaEvent.journalpostId}",)
    }

    private fun createOppgave(enrichedKafkaEvent: EnrichedKafkaEvent): Oppgave {
        val isToFordeling = isJournalpostToFordeling(enrichedKafkaEvent)
        val desiredResult = if (isToFordeling) FORDELINGSOPPGAVE else JOURNALORINGSOPPGAVE
        if (!isToFordeling) {
            try {
                val oppgave: Oppgave = createManuellJournalfoeringsoppgave(enrichedKafkaEvent)
                incProcessCounter(JOURNALORINGSOPPGAVE, desiredResult, oppgave.tema)
                return oppgave
            } catch (e: ExternalServiceException) {
                log.info("Kunne ikke opprette jfr-oppgave for journapost ${enrichedKafkaEvent.journalpostId}. Forsøker fordelingsoppgave", e)
            } catch (e: NullPointerException) {
                log.info("Kunne ikke opprette jfr-oppgave for journapost ${enrichedKafkaEvent.journalpostId}. Forsøker fordelingsoppgave", e)
            }
        }
        return createFordelingsoppgaveAfterFailedToCreateOppgave(enrichedKafkaEvent, desiredResult)
    }

    private fun createFordelingsoppgaveAfterFailedToCreateOppgave(
        enrichedKafkaEvent: EnrichedKafkaEvent,
        desiredResult: String
    ): Oppgave {
        return try {
            val oppgave: Oppgave = createFordelingsoppgave(enrichedKafkaEvent)
            incProcessCounter(oppgave.oppgavetype, desiredResult, oppgave.tema)
            oppgave
        } catch (e: ExternalServiceException) {
            if (enrichedKafkaEvent.numFailedAttempts > 10) {
                enrichedKafkaEvent.identer = emptyList()
                return createFordelingsoppgave(enrichedKafkaEvent)
            }
            throw e
        }
    }

    private fun createManuellJournalfoeringsoppgave(enrichedKafkaEvent: EnrichedKafkaEvent): Oppgave {
        val journalpost: Journalpost = enrichedKafkaEvent.journalpost!!
        val oppgaveData = CreateOppgaveData(
            journalpostId = journalpost.journalpostId,
            tema = journalpost.tema,
            behandlingstema = journalpost.behandlingstema,
            behandlingstype = journalpost.behandlingstype,
            oppgavetype = JOURNALORINGSOPPGAVE,
            tildeltEnhetsnr = journalpost.journalforendeEnhet,
            beskrivelse = journalpost.tittel,
            frist = 1
        ).apply {
            if (enrichedKafkaEvent.isPersonbruker) {
                aktoerId = enrichedKafkaEvent.getId()
            } else {
                orgnr = enrichedKafkaEvent.getId()
            }
        }

        return try {
            oppgaveClient.createOppgave(oppgaveData)
        } catch (e: ExternalServiceException) {
            if (isErrorInvalidEnhet(e.feilmelding)) {
                log.warn("Klarte ikke opprette oppgave pga ugyldig enhet på journalpost ${enrichedKafkaEvent.journalpostId}")
                oppgaveData.removeJournalforendeEnhet()
                return oppgaveClient.createOppgave(oppgaveData)
            } else if (isErrorInvalidOrgNr(e.feilmelding)) {
                log.warn("Klarte ikke opprette oppgave pga ugyldig OrgNr på journalpost ${enrichedKafkaEvent.journalpostId}")
                oppgaveData.removeOrgNr()
                return oppgaveClient.createOppgave(oppgaveData)
            }
            throw e
        }
    }

    private fun createFordelingsoppgave(enrichedKafkaEvent: EnrichedKafkaEvent): Oppgave {
        val requestData = CreateOppgaveData(
            journalpostId = enrichedKafkaEvent.journalpostId,
            oppgavetype = FORDELINGSOPPGAVE,
            frist = 1
        ).apply {
            if (enrichedKafkaEvent.isPersonbruker) {
                aktoerId = enrichedKafkaEvent.getId()
            } else {
                orgnr = enrichedKafkaEvent.getId()
            }

            if (TEMA_UKJENT == enrichedKafkaEvent.tema) {
                tema = TEMA_GENERELL
            } else {
                tema = enrichedKafkaEvent.tema
            }
        }
        return oppgaveClient.createOppgave(requestData)
    }

    fun isJournalpostToFordeling(enrichedKafkaEvent: EnrichedKafkaEvent): Boolean {
        val journalpost = enrichedKafkaEvent.journalpost
        if (journalpost == null) {
            log.error("Journalpost ${enrichedKafkaEvent.journalpostId} er null men er ikke markert til fordeling - sendes til fordeling")
            return true
        }
        return enrichedKafkaEvent.toFordeling || TEMA_UKJENT == journalpost.tema
    }

    private fun isErrorInvalidEnhet(feilmelding: String): Boolean {
        return (
            feilmelding.contains("NAVEnheten '") && feilmelding.contains("' er av typen oppgavebehandler") ||
                feilmelding.contains("NAVEnheten '") && feilmelding.contains("' har status: 'Nedlagt'") ||
                feilmelding.contains("Enheten med nummeret '") && feilmelding.contains("' eksisterer ikke")
            )
    }

    private fun isErrorInvalidOrgNr(feilmelding: String?): Boolean {
        return (feilmelding != null && feilmelding.contains("Organisasjonsnummer er ugyldig"))
    }

    companion object {
        private val log = LoggerFactory.getLogger(ManuellOperations::class.java)
        private const val FORDELINGSOPPGAVE = "FDR"
        private const val JOURNALORINGSOPPGAVE = "JFR"
        private const val TEMA_UKJENT = "UKJ"
        private const val TEMA_GENERELL = "GEN"
    }
}
