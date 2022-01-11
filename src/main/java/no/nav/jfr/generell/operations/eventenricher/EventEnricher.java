package no.nav.jfr.generell.operations.eventenricher;

import no.nav.jfr.generell.infrastructure.exceptions.*;
import no.nav.jfr.generell.infrastructure.metrics.Metrics;
import no.nav.jfr.generell.operations.SkjemaMetadata;
import no.nav.jfr.generell.operations.eventenricher.journalpost.Journalpost;
import no.nav.jfr.generell.operations.eventenricher.pdl.Ident;
import no.nav.jfr.generell.operations.eventenricher.pdl.PdlClient;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.jfr.generell.infrastructure.kafka.KafkaEvent;
import no.nav.jfr.generell.operations.eventenricher.saf.SafClient;
import no.nav.jfr.generell.operations.generell.felleskodeverk.FkvClient;
import no.nav.jfr.generell.operations.generell.felleskodeverk.FkvKrutkoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EventEnricher {
    private static final Logger log = LoggerFactory.getLogger(EventEnricher.class);
    private static final long KRUT_KODER_REFRESH_INTERVAL_HOURS = 5L;
    private final SafClient safClient;
    private final PdlClient pdlClient;
    private final FkvClient fkvClient;
    private FkvKrutkoder fkvKrutkoder;
    private SkjemaMetadata skjemaMetadata;

    public EventEnricher() throws Exception {
        this.safClient = new SafClient();
        this.pdlClient = new PdlClient();
        this.fkvClient = new FkvClient();
        this.skjemaMetadata = new SkjemaMetadata();
        this.fkvKrutkoder = fkvClient.fetchKrutKoder();
        this.setupRefreshFellesKodeverkTimer();
    }

    public void createEnrichedKafkaEvent(final EnrichedKafkaEvent enrichedKafkaEvent) throws Exception{
        enrichJournalpostInKafkaEvent(enrichedKafkaEvent);
        updateBehandlingValues(enrichedKafkaEvent);
        if(enrichedKafkaEvent.isPersonbruker()){
            enrichIdenterFromPDL(enrichedKafkaEvent);
        }else{
            log.info("Kaller ikke PDL da bruker på journalpost {} er ikke en person", enrichedKafkaEvent.getJournalpostId());
        }
    }

    private void enrichJournalpostInKafkaEvent(final EnrichedKafkaEvent enrichedKafkaEvent)throws Exception{
        final Journalpost journalpost = retriveJournalpostFromSaf(enrichedKafkaEvent.getKafkaEvent());
        enrichedKafkaEvent.setJournalpost(journalpost);
        if(journalpost.invalidJournalpostStatus()){
            log.info("Avslutter videre behandling da journalpost {} har status {}", journalpost.getJournalpostId(), journalpost.getJournalstatus());
            Metrics.incInvalidJournalpostStatus(enrichedKafkaEvent);
            throw new InvalidJournalpostStatusException();
        }
        if(skjemaMetadata.isIgnoreskjema(enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getSkjema())){
            log.info("Avslutter videre behandling da journalpost {} har skjema {} på tema {} som eksplisitt skal ignoreres!", enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getSkjema(), enrichedKafkaEvent.getTema());
            throw new InvalidJournalpostStatusException();
        }
        enrichedKafkaEvent.setJournalpost(journalpost);
    }

    private Journalpost retriveJournalpostFromSaf(final KafkaEvent kafkaEvent) throws TemporarilyUnavailableException, ExternalServiceException {
        try {
            final Journalpost journalpost = safClient.retriveJournalpost(kafkaEvent.getJournalpostId());
            log.info("Hentet {} fra Saf", journalpost.toString());
            return journalpost;
        } catch (final ExternalServiceException se) {
            log.error("Kunne ikke hente journalpost: {} fra Saf" , kafkaEvent.getJournalpostId());
            throw se;
        }
    }

    private void enrichIdenterFromPDL(EnrichedKafkaEvent enrichedKafkaEvent) throws Exception {
        Journalpost journalpost = enrichedKafkaEvent.getJournalpost();
        if(journalpost.getBruker() == null){
            log.info("Bruker er ikke satt på journalpost: {}. Kan ikke hente fra PDL", journalpost.getJournalpostId());
            throw new FunctionalRequirementException();
        }
        final List<Ident> identer = pdlClient.retrieveIdenterFromPDL(journalpost.getBruker().getId(), journalpost.getTema(), enrichedKafkaEvent.getJournalpostId());
        enrichedKafkaEvent.setIdenter(identer);
        log.info("Hentet alle id-nummer for bruker på journalpost: {} fra PDL", journalpost.getJournalpostId());
    }

    private void updateBehandlingValues(final EnrichedKafkaEvent enrichedKafkaEvent) throws FunctionalRequirementException {
        final Journalpost journalpost = enrichedKafkaEvent.getJournalpost();
        try {
            String behandlingsTema = fkvKrutkoder.getBehandlingstema(journalpost.getTema(), journalpost.getBrevkode());
            String behandlingsType = fkvKrutkoder.getBehandlingstype(journalpost.getTema(), journalpost.getBrevkode());
            if (!(journalpost.getBehandlingstema() == null || journalpost.getBehandlingstema().isEmpty())) {
                behandlingsTema = journalpost.getBehandlingstema();
            }
            journalpost.setBehandlingstema(behandlingsTema);
            journalpost.setBehandlingstype(behandlingsType);
            log.info("Satt følgende verdier behandlingstema: '{}' og behandlingstype: '{}' på journalpost {}", behandlingsTema, behandlingsType, journalpost.getJournalpostId());
        } catch (Exception e) {
            enrichedKafkaEvent.setToFordeling(true);
            enrichedKafkaEvent.setToManuell(true);
            log.warn("Klarte ikke finne behandlingsverdier for journalpost {} med tema {} og skjema {}", journalpost.getJournalpostId(), journalpost.getTema(), journalpost.getBrevkode(), e);
        }
    }

    private void setupRefreshFellesKodeverkTimer() {
        final TimerTask fellesKodeverkRefresher = new TimerTask() {
            @Override
            public void run() {
                try {
                    fkvKrutkoder = fkvClient.fetchKrutKoder();
                } catch (Exception e) {
                    log.error("Kunne ikke oppdatere KrutKoder fra FKV. Bruker gamle koder og prøver på nytt senere");
                }
            }
        };
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        long refresherDelay = KRUT_KODER_REFRESH_INTERVAL_HOURS;
        executor.scheduleAtFixedRate(fellesKodeverkRefresher, refresherDelay, refresherDelay, TimeUnit.HOURS);
    }
}
