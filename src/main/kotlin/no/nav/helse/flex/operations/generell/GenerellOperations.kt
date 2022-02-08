package no.nav.helse.flex.operations.generell

import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incFailFunctionalRequirements
import no.nav.helse.flex.operations.SkjemaMetadata
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.slf4j.LoggerFactory

class GenerellOperations(
    private val oppgaveClient: OppgaveClient = OppgaveClient()
) {

    fun executeProcess(enrichedKafkaEvent: EnrichedKafkaEvent) {
        if (checkFunctionalRequirements(enrichedKafkaEvent)) {
            createOppgave(enrichedKafkaEvent)
        } else {
            throw FunctionalRequirementException()
        }
    }

    private fun checkFunctionalRequirements(enrichedKafkaEvent: EnrichedKafkaEvent): Boolean {
        return hasValidDokumentTitler(enrichedKafkaEvent)
    }

    private fun createOppgave(enrichedKafkaEvent: EnrichedKafkaEvent) {
        if (!enrichedKafkaEvent.hasOppgave()) {
            val journalpost = enrichedKafkaEvent.journalpost!!

            val behandlingstema = journalpost.behandlingstema
            val behandlingstype = journalpost.behandlingstype
            val oppgavetype = SkjemaMetadata.getOppgavetype(enrichedKafkaEvent.tema, enrichedKafkaEvent.skjema)
            val frist = SkjemaMetadata.getFrist(enrichedKafkaEvent.tema, enrichedKafkaEvent.skjema)

            val requestData = CreateOppgaveData(
                aktoerId = enrichedKafkaEvent.aktoerId,
                journalpostId = journalpost.journalpostId,
                tema = journalpost.tema,
                behandlingstema = behandlingstema,
                behandlingstype = behandlingstype,
                oppgavetype = oppgavetype,
                frist = frist
            )

            if (journalpost.journalforendeEnhet != null && journalpost.journalforendeEnhet!!.isNotBlank()) {
                requestData.tildeltEnhetsnr = journalpost.journalforendeEnhet
            }

            val oppgave = oppgaveClient.createOppgave(requestData)
            enrichedKafkaEvent.oppgave = oppgave
            log.info("Opprettet oppgave: ${oppgave.id} for journalpost: ${enrichedKafkaEvent.journalpostId}")
        }
    }

    private fun hasValidDokumentTitler(enrichedKafkaEvent: EnrichedKafkaEvent): Boolean {
        for (dokument in enrichedKafkaEvent.journalpost!!.dokumenter) {
            if (dokument.tittel.isNullOrEmpty()) {
                incFailFunctionalRequirements("TITTEL", enrichedKafkaEvent)
                log.info(
                    "Avbryter automatisk behandling. " +
                        "Journalpost ${enrichedKafkaEvent.journalpostId} har dokument " +
                        "${dokument.dokumentInfoId} med tittel ${dokument.tittel}"
                )
                return false
            }
        }
        return true
    }

    companion object {
        private val log = LoggerFactory.getLogger(GenerellOperations::class.java)
    }
}
