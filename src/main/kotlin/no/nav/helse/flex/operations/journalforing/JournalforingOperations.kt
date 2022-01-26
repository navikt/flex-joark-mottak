package no.nav.helse.flex.operations.journalforing

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.journalforing.dokarkiv.JournalpostAPIClient
import org.slf4j.LoggerFactory

class JournalforingOperations(
    private val journalpostAPIClient: JournalpostAPIClient = JournalpostAPIClient()
) {
    private val log = LoggerFactory.getLogger(JournalforingOperations::class.java)

    fun doAutomaticStuff(enrichedKafkaEvent: EnrichedKafkaEvent) {
        enrichedKafkaEvent.journalpost!!.updateWithGenerellSak()
        populateAvsendeMottaker(enrichedKafkaEvent.journalpost!!, enrichedKafkaEvent.fnr!!)
        updateJournalpost(enrichedKafkaEvent.journalpost!!)
        finalizeJournalpost(enrichedKafkaEvent.journalpostId)
    }

    private fun populateAvsendeMottaker(journalpost: Journalpost, fnr: String) {
        if (journalpost.avsenderMottaker?.id == null) {
            journalpost.settAvsenderMottaker(fnr, "FNR")
            log.info("Setter bruker som avsender på journalpost: ${journalpost.journalpostId}")
        }
    }

    private fun updateJournalpost(journalpost: Journalpost) {
        journalpostAPIClient.updateJournalpost(journalpost)
        log.info("Oppdatert $journalpost")
    }

    private fun finalizeJournalpost(journalpostId: String) {
        journalpostAPIClient.finalizeJournalpost(journalpostId)
        log.info("Ferdigstilt og fullført behandling av journalpost $journalpostId.")
    }
}
