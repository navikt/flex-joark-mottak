package no.nav.jfr.generell.operations.journalforing;

import no.nav.jfr.generell.infrastructure.exceptions.ExternalServiceException;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.jfr.generell.operations.eventenricher.journalpost.Journalpost;
import no.nav.jfr.generell.operations.journalforing.dokarkiv.JournalpostAPIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalforingOperations {
    private static final Logger log = LoggerFactory.getLogger(JournalforingOperations.class);
    private final JournalpostAPIClient journalpostAPIClient;

    public JournalforingOperations(){
        this.journalpostAPIClient = new JournalpostAPIClient();
    }

    public void doAutomaticStuff(final EnrichedKafkaEvent enrichedKafkaEvent) throws TemporarilyUnavailableException, ExternalServiceException {
        enrichedKafkaEvent.getJournalpost().updateWithGenerellSak();
        populateAvsendeMottaker(enrichedKafkaEvent);
        journalpostAPIClient.updateJournalpost(enrichedKafkaEvent.getJournalpost());
        log.info("Oppdatert {}", enrichedKafkaEvent.getJournalpost().toString());
        journalpostAPIClient.finalizeJournalpost(enrichedKafkaEvent.getJournalpostId());
        log.info("Ferdigstilt og fullført behandling av journalpost {}.", enrichedKafkaEvent.getJournalpostId());
    }

    private void populateAvsendeMottaker(final EnrichedKafkaEvent enrichedKafkaEvent) {
        final Journalpost journalpost = enrichedKafkaEvent.getJournalpost();
        if (journalpost.getAvsenderMottaker() == null || journalpost.getAvsenderMottaker().getId() == null) {
            journalpost.settAvsenderMottaker(enrichedKafkaEvent.getFnr(), "FNR");
            log.info("Setter bruker som avsender på journalpost: {}", journalpost.getJournalpostId());
        }
    }
}
