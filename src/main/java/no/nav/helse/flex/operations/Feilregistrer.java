package no.nav.helse.flex.operations;

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.infrastructure.metrics.Metrics;
import no.nav.helse.flex.operations.generell.oppgave.Oppgave;
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Feilregistrer {
    private static final Logger log = LoggerFactory.getLogger(Feilregistrer.class);
    private static final String STATUS_FEILREG = "FEILREGISTRERT";
    private static final String BESKRIVELSE = "Oppgave feilregistrert automatisk av jfr-generell.";
    private final OppgaveClient oppgaveClient;

    public Feilregistrer() {
        this.oppgaveClient = new OppgaveClient();
    }

    public void feilregistrerOppgave(String kafkaId, EnrichedKafkaEvent enrichedKafkaEvent){
        if(enrichedKafkaEvent.getOppgave() != null){
            try{
                MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
                Oppgave oppgave = enrichedKafkaEvent.getOppgave();
                oppgave.setBeskrivelse(BESKRIVELSE);
                oppgave.setStatus(STATUS_FEILREG);
                enrichedKafkaEvent.setOppgave(oppgave);
                oppgaveClient.updateOppgave(enrichedKafkaEvent);
                log.info("Feilregistrerte oppgave: {} for journalpost: {}", enrichedKafkaEvent.getOppgaveId(), enrichedKafkaEvent.getJournalpostId());
                Metrics.incFeilregCounter(enrichedKafkaEvent, true);
            }catch (Exception e){
                log.error("Klarte ikke feilregistrere oppgave: {} tilhørende journalpost: {}. Sender likevel videre for opprettelse av manuell oppgave", enrichedKafkaEvent.getOppgaveId(), enrichedKafkaEvent.getJournalpostId(), e);
                Metrics.incFeilregCounter(enrichedKafkaEvent, false);
            }finally {
                MDC.clear();
            }
        }
    }

}
