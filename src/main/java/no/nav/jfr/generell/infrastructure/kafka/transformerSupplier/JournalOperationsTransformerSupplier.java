package no.nav.jfr.generell.infrastructure.kafka.transformerSupplier;

import no.nav.jfr.generell.infrastructure.exceptions.ExternalServiceException;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.jfr.generell.infrastructure.metrics.Metrics;
import no.nav.jfr.generell.operations.journalforing.JournalforingOperations;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;

public class JournalOperationsTransformerSupplier implements TransformerSupplier<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {

    private final static int MAX_NUM_RETRY = 5;
    private final String stateStoreName;
    private final JournalforingOperations journalOperations;
    private final static Logger log = LoggerFactory.getLogger(JournalOperationsTransformerSupplier.class);

    public JournalOperationsTransformerSupplier(String statestoreName) {
        this.journalOperations = new JournalforingOperations();
        this.stateStoreName = statestoreName;
    }

    @Override
    public Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> get() {
        return new Transformer<>() {
            private KeyValueStore<String, EnrichedKafkaEvent> stateStore;

            @SuppressWarnings("unchecked")
            @Override
            public void init(final ProcessorContext context) {
                this.stateStore = (KeyValueStore<String, EnrichedKafkaEvent>)context.getStateStore(stateStoreName);

                context.schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
                    stateStore.all().forEachRemaining(keyValue -> {
                        String id = keyValue.key;
                        EnrichedKafkaEvent enrichedKafkaEvent = keyValue.value;
                        boolean completeSendToStream = doOperations(enrichedKafkaEvent);
                        if(completeSendToStream){
                            stateStore.delete(id);
                            context.forward(id, enrichedKafkaEvent);
                            context.commit();
                        }else{
                            stateStore.put(id, enrichedKafkaEvent);
                        }
                    });
                });
            }

            @Override
            public KeyValue<String, EnrichedKafkaEvent> transform(String id, EnrichedKafkaEvent enrichedKafkaEvent) {
                KeyValue<String, EnrichedKafkaEvent> keyValue = null;
                boolean sendToStream = doOperations(enrichedKafkaEvent);
                if(sendToStream){
                    keyValue = KeyValue.pair(id, enrichedKafkaEvent);
                }else{
                    this.stateStore.putIfAbsent(id, enrichedKafkaEvent);
                }
                return keyValue;
           }

            @Override
            public void close() {
                // Note: The store should NOT be closed manually here via `stateStore.close()`!
                // The Kafka Streams API will automatically close stores when necessary.
            }

            private boolean doOperations(EnrichedKafkaEvent enrichedKafkaEvent){
                MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
                try {
                    journalOperations.doAutomaticStuff(enrichedKafkaEvent);
                    return true;
                } catch (TemporarilyUnavailableException e){
                    enrichedKafkaEvent.incNumFailedAttempts();
                    if(enrichedKafkaEvent.getNumFailedAttempts() < MAX_NUM_RETRY){
                        Metrics.incRetry(stateStoreName, enrichedKafkaEvent);
                        log.info("Feilet under oppdatering/ferdigstilling av journalpost {} for gang nummer {}. Forsøker på nytt senere", enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getNumFailedAttempts());
                        return false;
                    }else{
                        log.info("Feilet under oppdatering/ferdigstilling av journalpost {} for gang nummer {}. Gir opp videre automatisk behandling", enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getNumFailedAttempts());
                        enrichedKafkaEvent.setToManuell(true);
                        return true;
                    }
                } catch (ExternalServiceException e) {
                    enrichedKafkaEvent.setToManuell(true);
                    return true;
                } catch (Exception e){
                    log.error("Uventet feil på journalpost {}",enrichedKafkaEvent.getJournalpostId(), e);
                    enrichedKafkaEvent.setToManuell(true);
                    return true;
                }finally {
                    MDC.clear();
                }
            }
        };
    }
}
