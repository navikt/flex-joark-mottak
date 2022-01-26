package no.nav.helse.flex.operations

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incFeilregCounter
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.lang.Exception

class Feilregistrer(
    private val oppgaveClient: OppgaveClient = OppgaveClient()
) {
    private val log = LoggerFactory.getLogger(Feilregistrer::class.java)
    private val STATUS_FEILREG = "FEILREGISTRERT"
    private val BESKRIVELSE = "Oppgave feilregistrert automatisk av flex-joark-mottak."

    fun feilregistrerOppgave(enrichedKafkaEvent: EnrichedKafkaEvent) {
        if (enrichedKafkaEvent.oppgave != null) {
            try {
                MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)

                val oppgave = enrichedKafkaEvent.oppgave!!
                oppgave.beskrivelse = BESKRIVELSE
                oppgave.status = STATUS_FEILREG
                enrichedKafkaEvent.oppgave = oppgave
                oppgaveClient.updateOppgave(enrichedKafkaEvent)

                log.info("Feilregistrerte oppgave: ${enrichedKafkaEvent.getOppgaveId()} for journalpost: ${enrichedKafkaEvent.journalpostId}")
                incFeilregCounter(enrichedKafkaEvent, true)
            } catch (e: Exception) {
                log.error(
                    "Klarte ikke feilregistrere oppgave: ${enrichedKafkaEvent.getOppgaveId()} tilhørende journalpost: ${enrichedKafkaEvent.journalpostId}. Sender likevel videre for opprettelse av manuell oppgave",
                    e
                )
                incFeilregCounter(enrichedKafkaEvent, false)
            } finally {
                MDC.clear()
            }
        }
    }
}
