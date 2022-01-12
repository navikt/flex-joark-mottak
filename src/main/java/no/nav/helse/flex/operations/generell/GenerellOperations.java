package no.nav.helse.flex.operations.generell;

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException;
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.infrastructure.metrics.Metrics;
import no.nav.helse.flex.operations.SkjemaMetadata;
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData;
import no.nav.helse.flex.operations.generell.oppgave.Oppgave;
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerellOperations {

    private static final Logger log = LoggerFactory.getLogger(GenerellOperations.class);
    private final OppgaveClient oppgaveClient;
    private SkjemaMetadata skjemaMetadata;



    public GenerellOperations() {
        this.oppgaveClient = new OppgaveClient();
        this.skjemaMetadata = new SkjemaMetadata();
    }

    public void executeProcess(final EnrichedKafkaEvent enrichedKafkaEvent) throws Exception{
        if(checkFunctionalRequirements(enrichedKafkaEvent)){
            createOppgave(enrichedKafkaEvent);
        }else {
            throw new FunctionalRequirementException();
        }
    }

    private Boolean checkFunctionalRequirements(final EnrichedKafkaEvent enrichedKafkaEvent) throws ExternalServiceException, TemporarilyUnavailableException {
        return hasValidDokumentTitler(enrichedKafkaEvent);
    }


    private void createOppgave(final EnrichedKafkaEvent enrichedKafkaEvent) throws ExternalServiceException, TemporarilyUnavailableException {
        if (!enrichedKafkaEvent.hasOppgave()) {
            final Journalpost journalpost = enrichedKafkaEvent.getJournalpost();
            String behandlingstema = journalpost.getBehandlingstema();
            String behandlingstype = journalpost.getBehandlingstype();
            String oppgavetype = skjemaMetadata.getOppgavetype(enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getSkjema());
            int frist = skjemaMetadata.getFrist(enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getSkjema());
            final CreateOppgaveData requestData = new CreateOppgaveData(enrichedKafkaEvent.getAktoerId(), journalpost.getJournalpostId(),
                    journalpost.getTema(), behandlingstema, behandlingstype, oppgavetype, frist);
            if(journalpost.getJournalforendeEnhet() != null && !journalpost.getJournalforendeEnhet().isBlank()){
                requestData.setTildeltEnhetsnr(journalpost.getJournalforendeEnhet());
            }

            // TODO: Slå på når vi skal behandle
            //final Oppgave oppgave = oppgaveClient.createOppgave(requestData);
            //enrichedKafkaEvent.setOppgave(oppgave);

            log.info("Opprettet oppgave: {} for journalpost: {}", "id", enrichedKafkaEvent.getJournalpostId());
        }
    }

    private boolean hasValidDokumentTitler(final EnrichedKafkaEvent enrichedKafkaEvent){
        for (Dokument dokument: enrichedKafkaEvent.getJournalpost().getDokumenter()){
            if(dokument.getTittel() == null || dokument.getTittel().isEmpty()){
                Metrics.incFailFunctionalRequirements("TITTEL", enrichedKafkaEvent);
                log.info("Avbryter automatisk behandling. Journalpost {} har dokument {} med tittel {}",
                        enrichedKafkaEvent.getJournalpostId(), dokument.getDokumentInfoId(), dokument.getTittel());
                return false;
            }
        }
        return true;
    }
}
