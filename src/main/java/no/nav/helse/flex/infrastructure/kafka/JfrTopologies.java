package no.nav.helse.flex.infrastructure.kafka;

import com.google.gson.Gson;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier;
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier;
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier;
import no.nav.helse.flex.operations.Feilregistrer;
import no.nav.helse.flex.operations.SkjemaMetadata;
import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.metrics.Metrics;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JfrTopologies {
    private static final Logger log = LoggerFactory.getLogger(JfrTopologies.class);
    private static final List<String> eventType = Arrays.asList("MidlertidigJournalført", "Mottatt", "JournalpostMottatt");

    private final String inputTopic;
    private final String manuellTopic;
    private final String ENRICHER_OPERATION_STORE = "enrichoperations";
    private final String GENERELL_OPERATION_STORE = "generelloperations";
    private final String JOURNALFOERING_OPERATION_STORE = "journalfoeringoperations";
    private final Serde<KafkaEvent> kafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(KafkaEvent.class));
    private final Serde<EnrichedKafkaEvent> enhancedKafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(EnrichedKafkaEvent.class));

    private final StoreBuilder<KeyValueStore<String, EnrichedKafkaEvent>> eventEnricherSupplier;
    private final StoreBuilder<KeyValueStore<String, EnrichedKafkaEvent>> generellOperationSupplier;
    private final StoreBuilder<KeyValueStore<String, EnrichedKafkaEvent>> journalOperationSupplier;
    private final SkjemaMetadata skjemaMetadata;
    private final EventEnricherTransformerSupplier eventEnricherTransformerSupplier;
    private final GenerellOperationsTransformerSupplier generellOperationsTransformerSupplier;
    private final JournalOperationsTransformerSupplier journalOperationsTransformerSupplier;
    private final Feilregistrer feilregistrer;

    public JfrTopologies(final String inputTopic, final String manuellTopic) throws Exception {
        this.inputTopic = inputTopic;
        this.manuellTopic = manuellTopic;
        this.skjemaMetadata = new SkjemaMetadata();
        this.eventEnricherTransformerSupplier = new EventEnricherTransformerSupplier(ENRICHER_OPERATION_STORE);
        this.generellOperationsTransformerSupplier = new GenerellOperationsTransformerSupplier(GENERELL_OPERATION_STORE);
        this.journalOperationsTransformerSupplier = new JournalOperationsTransformerSupplier(JOURNALFOERING_OPERATION_STORE);
        this.feilregistrer = new Feilregistrer();
        this.eventEnricherSupplier = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(ENRICHER_OPERATION_STORE),
                Serdes.String(),
                enhancedKafkaEventSerde);
        this.generellOperationSupplier = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(GENERELL_OPERATION_STORE),
                Serdes.String(),
                enhancedKafkaEventSerde);
        this.journalOperationSupplier = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(JOURNALFOERING_OPERATION_STORE),
                Serdes.String(),
                enhancedKafkaEventSerde);
    }

    public Topology getJfrTopologi() {
        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        final Map<String, String> serdeConfig = Environment.getKafkaSerdeConfig();
        final Serde<GenericRecord> valueGenericAvroSerde = new GenericAvroSerde();
        valueGenericAvroSerde.configure(serdeConfig, false); // `false` for record values
        streamsBuilder.addStateStore(eventEnricherSupplier);
        streamsBuilder.addStateStore(generellOperationSupplier);
        streamsBuilder.addStateStore(journalOperationSupplier);

        final KStream<String, GenericRecord> aapenDokStream = streamsBuilder.stream(inputTopic, Consumed.with(Serdes.String(), valueGenericAvroSerde));
        final KStream<String, GenericRecord> filteredEvents = filterGenerelleEvent(aapenDokStream);
        final KStream<String, KafkaEvent> kafkaEventKStream = convertGenericRecordToKafkaEvent(filteredEvents);
        final KStream<String, EnrichedKafkaEvent> enrichedEventsStream = enrichKafkaEvent(kafkaEventKStream);

        final KStream<String, EnrichedKafkaEvent> filterJournalpostToAuto = filterJournalpostToAuto(enrichedEventsStream);
        final KStream<String, EnrichedKafkaEvent> processedGenerellStream = generellOperations(filterJournalpostToAuto);
        journalfoerJournalpost(processedGenerellStream);
        return streamsBuilder.build();
    }

    private KStream<String, GenericRecord> filterGenerelleEvent(final KStream<String, GenericRecord> inputStream) {
        return inputStream
                .filter((key, genericRecord) ->
                        genericRecord.get("temaNytt").toString().equals("SYK")
                        && eventType.contains(genericRecord.get("hendelsesType").toString()));
    }

    private KStream<String, KafkaEvent> convertGenericRecordToKafkaEvent(final KStream<String, GenericRecord> genericRecordString) {
        final KStream<String, KafkaEvent> kafkaEventKStream = genericRecordString.mapValues((genericRecord -> new Gson().fromJson(genericRecord.toString(), KafkaEvent.class)));
        return kafkaEventKStream;
    }

    private KStream<String, EnrichedKafkaEvent> enrichKafkaEvent(final KStream<String, KafkaEvent> kafkaEventKStream) {
        final KStream<String, EnrichedKafkaEvent> enrichedKafkaEvent = kafkaEventKStream.transform(eventEnricherTransformerSupplier, ENRICHER_OPERATION_STORE);
        final KStream<String, EnrichedKafkaEvent> toManuell = enrichedKafkaEvent.filter(((s, jfrEnhancedKafkaEvent) -> jfrEnhancedKafkaEvent.isToManuell() && !jfrEnhancedKafkaEvent.isToIgnore()));
        sendToJfrManuellOppretter(toManuell);
        return enrichedKafkaEvent.filter(((s, jfrEnhancedKafkaEvent) -> !jfrEnhancedKafkaEvent.isToManuell() && !jfrEnhancedKafkaEvent.isToIgnore()));
    }

    private KStream<String, EnrichedKafkaEvent> filterJournalpostToAuto(final KStream<String, EnrichedKafkaEvent> enrichedKafkaEventStream) {
        final KStream<String, EnrichedKafkaEvent> toAuto = enrichedKafkaEventStream.filter(((s, jfrEnhancedKafkaEvent) -> skjemaMetadata.inAutoList(jfrEnhancedKafkaEvent.getTema(), jfrEnhancedKafkaEvent.getSkjema())))
                .peek((k, enrichedKafkaEvent) -> logWithCorrelationId(enrichedKafkaEvent, "Forsøker automatisk behandling på journalpost {} med tema {} og skjema {}",
                        enrichedKafkaEvent.getJournalpostId(), enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getSkjema()));
        final KStream<String, EnrichedKafkaEvent> toManuell = enrichedKafkaEventStream.filterNot(((s, jfrEnhancedKafkaEvent) -> skjemaMetadata.inAutoList(jfrEnhancedKafkaEvent.getTema(), jfrEnhancedKafkaEvent.getSkjema())));
        sendToJfrManuellOppretter(toManuell);
        return toAuto;
    }

    private KStream<String, EnrichedKafkaEvent> generellOperations(final KStream<String, EnrichedKafkaEvent> enrichedKafkaEventStream) {
        final KStream<String, EnrichedKafkaEvent> afterGenerellOperationStream = enrichedKafkaEventStream.transform(generellOperationsTransformerSupplier, GENERELL_OPERATION_STORE);
        final KStream<String, EnrichedKafkaEvent> toManuell = afterGenerellOperationStream.filter((k, enrichedKafkaEvent) -> enrichedKafkaEvent.isToManuell());
        sendToJfrManuellOppretter(toManuell);
        return afterGenerellOperationStream.filterNot((k, enrichedKafkaEvent) -> enrichedKafkaEvent.isToManuell());
    }

    private void journalfoerJournalpost(final KStream<String, EnrichedKafkaEvent> oppgaveFilteredStream) {
        final KStream<String, EnrichedKafkaEvent> journalpostAfterJournalfoering = oppgaveFilteredStream.transform(journalOperationsTransformerSupplier, JOURNALFOERING_OPERATION_STORE);
        final KStream<String, EnrichedKafkaEvent> toManuell = journalpostAfterJournalfoering.filter((k, enrichedKafkaEvent) -> enrichedKafkaEvent.isToManuell());
        final KStream<String, EnrichedKafkaEvent> doneProcessing = journalpostAfterJournalfoering.filterNot((k, enrichedKafkaEvent) -> enrichedKafkaEvent.isToManuell());
        doneProcessing.peek((s, enrichedKafkaEvent) -> Metrics.incJfrAutoProcess(enrichedKafkaEvent));
        sendToJfrManuellOppretter(toManuell);
    }

    private void sendToJfrManuellOppretter(final KStream<String, EnrichedKafkaEvent> manuelle) {
        manuelle.foreach(feilregistrer::feilregistrerOppgave);
        manuelle.peek((s, enrichedKafkaEvent) -> Metrics.incJfrManuallProcess(enrichedKafkaEvent, skjemaMetadata.inAutoList(enrichedKafkaEvent.getTema(), enrichedKafkaEvent.getSkjema())));
        manuelle.peek((k, enrichKafkaEvent) -> logWithCorrelationId(enrichKafkaEvent, "Journalposten: {} sendes til manuell-oppretter", enrichKafkaEvent.getJournalpostId()));
                // TODO: Slå på når topic og app er opprettet
                // .to(manuellTopic, Produced.with(Serdes.String(), enhancedKafkaEventSerde));
    }

    private void logWithCorrelationId(EnrichedKafkaEvent enrichedKafkaEvent, String s, String... args) {
        MDC.put("CORRELATION_ID", enrichedKafkaEvent.getCorrelationId());
        log.info(s, (Object[]) args);
        MDC.clear();
    }

}
