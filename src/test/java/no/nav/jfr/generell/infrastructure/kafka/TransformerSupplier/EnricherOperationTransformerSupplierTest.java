package no.nav.jfr.generell.infrastructure.kafka.TransformerSupplier;

import no.nav.jfr.generell.infrastructure.exceptions.InvalidJournalpostStatusException;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.kafka.*;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier;
import no.nav.jfr.generell.operations.eventenricher.EventEnricher;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JfrTopologies.class, EventEnricherTransformerSupplier.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class EnricherOperationTransformerSupplierTest {
    private final String INPUT_TOPIC = "source-topic";
    private final String OUTPUT_TOPIC = "output-topic";
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, KafkaEvent> inputTopic;
    private TestOutputTopic<String, KafkaEvent> jfr_manuell_outputTopic;
    private EventEnricher eventEnricher;
    private final String ENRICHER_OPERATION_STORE = "enrichoperations";
    private final Serde<EnrichedKafkaEvent> enhancedKafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(EnrichedKafkaEvent.class));
    private final Serde<KafkaEvent> kafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(KafkaEvent.class));
    private final static int MAX_RETRY = 5;
    KeyValueStore<String, EnrichedKafkaEvent> enricherKVStore;

    @Before
    public void setup() throws Exception {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        eventEnricher = mock(EventEnricher.class);
        PowerMockito.whenNew(EventEnricher.class).withNoArguments().thenReturn(eventEnricher);

        testDriver = new TopologyTestDriver(testEnricherTopology(), props);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), new JfrKafkaSerializer<>());
        jfr_manuell_outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new JfrKafkaDeserializer<>(KafkaEvent.class));
        this.enricherKVStore = testDriver.getKeyValueStore(ENRICHER_OPERATION_STORE);
    }

    private Topology testEnricherTopology() throws Exception {
        StoreBuilder<KeyValueStore<String, EnrichedKafkaEvent>> eventEnricherSupplier = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(ENRICHER_OPERATION_STORE),
                Serdes.String(),
                enhancedKafkaEventSerde);
        EventEnricherTransformerSupplier eventEnricherTransformerSupplier = new EventEnricherTransformerSupplier(ENRICHER_OPERATION_STORE);

        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder.addStateStore(eventEnricherSupplier);
        final KStream<String, KafkaEvent> inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde));
        final KStream<String, EnrichedKafkaEvent> enrichedKafkaEvent = inputStream.transform(eventEnricherTransformerSupplier, ENRICHER_OPERATION_STORE);
        enrichedKafkaEvent
                .filter((k, enrichedEvent) -> enrichedEvent.isToManuell())
                .map((k, infotrygdJournalpostData) -> KeyValue.pair(k, infotrygdJournalpostData.getKafkaEvent()))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde));
        return streamsBuilder.build();
    }

    private KafkaEvent getTestEvent(){
        return new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
    }

    @Test
    public void test_when_enricher_catch_unknownException_send_to_manuell() throws Exception {
        KafkaEvent event = getTestEvent();
        doThrow(new Exception("Unknown Error")).when(eventEnricher).createEnrichedKafkaEvent(any(EnrichedKafkaEvent.class));
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent = enricherKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals("123456789", jfr_manuell_outputTopic.readValue().getJournalpostId());
    }

    @Test
    public void test_journalpost_get_sent_to_enricher_fail_send_to_keyValueStore() throws Exception {
        doThrow(new TemporarilyUnavailableException()).when(eventEnricher).createEnrichedKafkaEvent(any(EnrichedKafkaEvent.class));
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent enrichedKafkaEvent = enricherKVStore.get("Test123");
        assertEquals("123456789", enrichedKafkaEvent.getJournalpostId());
    }

    @Test
    public void test_journalpost_enricher_fail_more_than_max_retry_send_to_manuell() throws Exception {
        doThrow(new TemporarilyUnavailableException()).when(eventEnricher).createEnrichedKafkaEvent(any(EnrichedKafkaEvent.class));
        KafkaEvent event = getTestEvent();
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent;
        for (int i = 1; i < MAX_RETRY; i++) {
            kafkaEvent = enricherKVStore.get("Test123");
            assertEquals(kafkaEvent.getJournalpostId(), "123456789");
            testDriver.advanceWallClockTime(Duration.ofMinutes(30));
            // kafkaEvent store i KV-store when SUE

        }
        // after max retry attempt - Journlapost should be send to manuell and store empty
        kafkaEvent = enricherKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals("123456789", jfr_manuell_outputTopic.readValue().getJournalpostId());
    }

    @Test
    public void test_journalpost_enricher_fail_SUE_then_unknown_exception_send_to_manuell() throws Exception {
        doThrow(new TemporarilyUnavailableException()).when(eventEnricher).createEnrichedKafkaEvent(any(EnrichedKafkaEvent.class));
        KafkaEvent event = getTestEvent();
        inputTopic.pipeInput("Test123", event);
        // check store have journalpost staored after SUE
        EnrichedKafkaEvent kafkaEvent = enricherKVStore.get("Test123");
        assertEquals(kafkaEvent.getJournalpostId(), "123456789");
        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        doThrow(new Exception("Ukjent feil")).when(eventEnricher).createEnrichedKafkaEvent(any(EnrichedKafkaEvent.class));
        testDriver.advanceWallClockTime(Duration.ofMinutes(30));
        kafkaEvent = enricherKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals("123456789", jfr_manuell_outputTopic.readValue().getJournalpostId());
    }

    @Test
    public void test_journalpost_enricher_not_continiouProcess_if_SAF_status_is_J() throws Exception {
        doThrow(new InvalidJournalpostStatusException()).when(eventEnricher).createEnrichedKafkaEvent(any());
        KafkaEvent event = getTestEvent();
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent = enricherKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals(0, jfr_manuell_outputTopic.getQueueSize());
    }
}
