package no.nav.helse.flex.infrastructure.kafka.transformerSupplier;

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException;
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException;
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent;
import no.nav.helse.flex.infrastructure.metrics.Metrics;
import no.nav.helse.flex.operations.eventenricher.EventEnricher;
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

public class EventEnricherTransformerSupplier implements TransformerSupplier<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {

    private final EventEnricher eventEnricher;
    private final String stateStoreName;
    private final static Logger log = LoggerFactory.getLogger(EventEnricherTransformerSupplier.class);
    private final static int MAX_NUM_RETRY = 5;

    public EventEnricherTransformerSupplier(String statestoreName) throws Exception{
        this.stateStoreName = statestoreName;
        this.eventEnricher = new EventEnricher();
    }

    @Override
    public Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> get() {
        return new Transformer<>() {
            private KeyValueStore<String, EnrichedKafkaEvent> stateStore;

            @SuppressWarnings("unchecked")
            @Override
            public void init(final ProcessorContext context) {
                this.stateStore = (KeyValueStore<String, EnrichedKafkaEvent>)context.getStateStore(stateStoreName);

                context.schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME, timestamp -> {
                    stateStore.all().forEachRemaining(keyValue -> {
                        EnrichedKafkaEvent enrichedKafkaEvent = keyValue.value;
                        String id = keyValue.key;
                        boolean completeSendToStream = doOperations(enrichedKafkaEvent);

                        if(completeSendToStream){
                            stateStore.delete(id);
                            context.forward(id, enrichedKafkaEvent);
                            context.commit();
                        }else{
                            stateStore.put(id, enrichedKafkaEvent);
                        }
                    });
                    Metrics.setRetrystoreGauge(stateStoreName, stateStore.approximateNumEntries());
                });
            }

            @Override
            public KeyValue<String, EnrichedKafkaEvent> transform(String id, KafkaEvent kafkaEvent) {
                EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(kafkaEvent);
                KeyValue<String, EnrichedKafkaEvent> keyValue = null;
                boolean sendToStream = doOperations(enrichedKafkaEvent);
                if(sendToStream){
                    keyValue = KeyValue.pair(id, enrichedKafkaEvent);
                }else if(!enrichedKafkaEvent.isToIgnore()){
                    this.stateStore.putIfAbsent(id, enrichedKafkaEvent);
                    keyValue = null;
                    Metrics.setRetrystoreGauge(stateStoreName, stateStore.approximateNumEntries());
                }
                return keyValue;
            }

            private boolean doOperations(final EnrichedKafkaEvent enrichedKafkaEvent){
                try {
                    MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
                    eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent);
                    return true;
                }catch (InvalidJournalpostStatusException e){
                    enrichedKafkaEvent.setToIgnore(true);
                    return true;
                } catch (FunctionalRequirementException e){
                    enrichedKafkaEvent.setToManuell(true);
                    return true;
                } catch (TemporarilyUnavailableException e){
                    enrichedKafkaEvent.incNumFailedAttempts();
                    if(enrichedKafkaEvent.getNumFailedAttempts() < MAX_NUM_RETRY){
                        Metrics.incRetry(stateStoreName, enrichedKafkaEvent);
                        log.info("Feilet under berikelse av journalpost {} for gang nummer {}. Forsøker på nytt senere", enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getNumFailedAttempts());
                        return false;
                    }else{
                        log.info("Feilet under berikelse av journalpost {} for gang nummer {}. Gir opp videre behandling", enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getNumFailedAttempts());
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

            @Override
            public void close() {
                // Note: The store should NOT be closed manually here via `stateStore.close()`!
                // The Kafka Streams API will automatically close stores when necessary.
            }
        };
    }
}
