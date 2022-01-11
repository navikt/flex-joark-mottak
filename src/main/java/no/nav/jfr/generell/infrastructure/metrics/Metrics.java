package no.nav.jfr.generell.infrastructure.metrics;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class Metrics {
    private static final Logger log = LoggerFactory.getLogger(Metrics.class);

    private static final String MANUELL = "Manuell journalforing";
    private static final String AUTO = "Automatisk journalforing";

    private static final Counter jfrProcessCounter = Counter.build(
            "jfr_generell_process_counter",
            "Teller antall journalposter som behandles av jfr-generell. Setter på labels på om det burde blitt behandlet maskinelt/manuelt og om den blir behandlet maskinelt/manuelt")
            .labelNames("result", "desired", "tema", "kanal", "skjema")
            .register();

    private static final Counter feilregOppgaverCounter = Counter.build(
            "jfr_generell_feilregistrerte_oppgaver_counter",
            "Teller antall ganger vi må avbryte autoatiske prosess på en slik måte at en oppgave må feilregistreres")
            .labelNames("ableToFeilreg", "tema", "kanal", "skjema")
            .register();

    private static final Counter functionalReqFailCounter = Counter.build(
            "jfr_generell_functional_req_fail_counter",
            "Teller funksjonelle hindringer for automatisk journalforing")
            .labelNames("reason", "tema", "kanal", "skjema")
            .register();

    private static final Counter invalidJournalpostStatusCounter = Counter.build(
            "jfr_generell_invalid_journalpost_status_counter",
            "Teller journalposter fra SAF med ugyldig status")
            .labelNames("status", "tema", "kanal", "skjema")
            .register();

    private static final Counter retryCounter = Counter.build(
            "jfr_generell_retry_counter",
            "Teller antall journalposter hver gang vi vil prøve på nytt senere")
            .labelNames("transformer", "numErrors", "tema", "kanal", "skjema")
            .register();

    private static final Gauge retrystoreGauge = Gauge.build(
            "jfr_generell_retry_gauge",
            "Viser hvor mange JP som ligger på retrystore")
            .labelNames("transformer")
            .register();

    public static void incJfrManuallProcess(EnrichedKafkaEvent enrichedKafkaEvent, boolean desiredAutomaticJfr){
        String desired = "";
        String skjema = "null";
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            desired = (desiredAutomaticJfr ? AUTO : MANUELL);
            if(enrichedKafkaEvent.getJournalpost() != null ){
                skjema = enrichedKafkaEvent.getSkjema();
            }
            jfrProcessCounter.labels(MANUELL, desired, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(skjema)).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk jfrProcessCounter med verdier: '{}' '{}' '{}' '{}' '{}'", MANUELL, desired, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), skjema, e);
        }
    }

    public static void incJfrAutoProcess(EnrichedKafkaEvent enrichedKafkaEvent){
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            jfrProcessCounter.labels(AUTO, AUTO, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema())).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk jfrProcessCounter med verdier: '{}' '{}' '{}' '{}' '{}'", MANUELL, AUTO, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema()), e);
        }
    }

    public static void incFeilregCounter(EnrichedKafkaEvent enrichedKafkaEvent, boolean sucessfullFeilreg){
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            feilregOppgaverCounter.labels(Boolean.toString(sucessfullFeilreg), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema())).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk feilregOppgaverCounter med verdier: '{}' '{}' '{}' '{}'", sucessfullFeilreg, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), enrichedKafkaEvent.getSkjema(), e);
        }
    }

    public static void incFailFunctionalRequirements(String reason, EnrichedKafkaEvent enrichedKafkaEvent){
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            functionalReqFailCounter.labels(reason, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema())).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk incFailFunctionalRequirements med verdier: '{}' '{}' '{}' '{}'", reason, enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), enrichedKafkaEvent.getSkjema(), e);
        }
    }

    public static void incInvalidJournalpostStatus(EnrichedKafkaEvent enrichedKafkaEvent){
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            invalidJournalpostStatusCounter.labels(enrichedKafkaEvent.getJournalpost().getJournalstatus(), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema())).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk incInvalidJournalpostStatus med verdier: '{}' '{}' '{}' '{}'", enrichedKafkaEvent.getJournalpost().getJournalstatus(), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), enrichedKafkaEvent.getSkjema(), e);
        }
    }

    public static void incRetry(String transformer, EnrichedKafkaEvent enrichedKafkaEvent){
        String skjema = "null";
        try {
            MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
            if(enrichedKafkaEvent.getJournalpost() != null ){
                skjema = enrichedKafkaEvent.getSkjema();
            }
            retryCounter.labels(transformer, String.valueOf(enrichedKafkaEvent.getNumFailedAttempts()), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), String.valueOf(enrichedKafkaEvent.getSkjema())).inc();
        }catch (Exception e){
            log.error("Klarte ikke å inkrementere metrikk retryCounter med verdier: '{}' '{}' '{}' '{}' '{}'", transformer, String.valueOf(enrichedKafkaEvent.getNumFailedAttempts()), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getKafkaEvent().getMottaksKanal(), skjema, e);
        }
    }

    public static void setRetrystoreGauge(String name, long num){
        try {
            retrystoreGauge.labels(name).set(num);
        }catch (Exception e){
            log.error("Klarte ikke sette retry gauge");
        }
    }
}
