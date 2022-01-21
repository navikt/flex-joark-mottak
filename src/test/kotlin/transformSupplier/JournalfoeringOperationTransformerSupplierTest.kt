package no.nav.helse.flex.infrastructure.kafka.TransformerSupplier;

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.kafka.*;
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier;
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.eventenricher.pdl.Ident;
import no.nav.helse.flex.operations.journalforing.JournalforingOperations;
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JfrTopologies.class, JournalOperationsTransformerSupplier.class, JournalforingOperations.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class JournalfoeringOperationTransformerSupplierTest {
    private final String INPUT_TOPIC = "source-topic";
    private final String OUTPUT_TOPIC = "output-topic";

    private TopologyTestDriver testDriver;
    private TestInputTopic<String, KafkaEvent> inputTopic;
    private TestOutputTopic<String, KafkaEvent> jfr_manuell_outputTopic;
    private JournalforingOperations journalforingOperations;
    private final String JOURNALFOERING_OPERATION_STORE = "journalfoeringoperations";
    KeyValueStore<String, EnrichedKafkaEvent> journalforingOperationsKVStore;
    private final Serde<EnrichedKafkaEvent> enhancedKafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(EnrichedKafkaEvent.class));
    private final Serde<KafkaEvent> kafkaEventSerde = Serdes.serdeFrom(new JfrKafkaSerializer<>(), new JfrKafkaDeserializer<>(KafkaEvent.class));
    private final static int MAX_RETRY = 5;

    @Before
    public void setup() throws Exception {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        journalforingOperations = mock(JournalforingOperations.class);
        PowerMockito.whenNew(JournalforingOperations.class).withNoArguments().thenReturn(journalforingOperations);

        testDriver = new TopologyTestDriver(testInfotrygdTopology(), props);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), new JfrKafkaSerializer<>());
        jfr_manuell_outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new JfrKafkaDeserializer<>(KafkaEvent.class));
        this.journalforingOperationsKVStore = testDriver.getKeyValueStore(JOURNALFOERING_OPERATION_STORE);
    }

    private Topology testInfotrygdTopology() {
        StoreBuilder<KeyValueStore<String, EnrichedKafkaEvent>> journalfoeringOperationSupplier = Stores.keyValueStoreBuilder(
                Stores.inMemoryKeyValueStore(JOURNALFOERING_OPERATION_STORE),
                Serdes.String(),
                enhancedKafkaEventSerde);
        JournalOperationsTransformerSupplier journalOperationsTransformerSupplier = new JournalOperationsTransformerSupplier(JOURNALFOERING_OPERATION_STORE);

        final StreamsBuilder streamsBuilder = new StreamsBuilder();
        streamsBuilder.addStateStore(journalfoeringOperationSupplier);
        final KStream<String, KafkaEvent> inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde));
        final KStream<String, EnrichedKafkaEvent> enrichedKafkaEvent = inputStream.mapValues(this::getEntrichKafkaEvent);
        final KStream<String, EnrichedKafkaEvent> postJournalfoeringKafkaEvent = enrichedKafkaEvent.transform(journalOperationsTransformerSupplier, JOURNALFOERING_OPERATION_STORE);
        postJournalfoeringKafkaEvent
                .filter((k, enrichedEvent) -> enrichedEvent.isToManuell())
                .map((k, infotrygdJournalpostData) -> KeyValue.pair(k, infotrygdJournalpostData.getKafkaEvent()))
                .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde));
        return streamsBuilder.build();
    }

    private Journalpost mockJournalpost(KafkaEvent event){
        Journalpost.Bruker mockBruker;
        mockBruker = new Journalpost.Bruker("1234", "FNR");
        Journalpost mockJournalpost = new Journalpost();
        mockJournalpost.setTittel("Test Journalpost");
        mockJournalpost.setJournalpostId(event.getJournalpostId());
        mockJournalpost.setJournalforendeEnhet("1111");
        mockJournalpost.setDokumenter(Collections.singletonList(new Dokument("NAV 08-36.05", "DokTittel", "123")));
        mockJournalpost.setBruker(mockBruker);
        mockJournalpost.setJournalstatus(event.getJournalpostStatus());
        mockJournalpost.setTema(event.getTemaNytt());
        return mockJournalpost;
    }

    private KafkaEvent getTestEvent(){
        return new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "FOS", "NAV_NO");

//        return new KafkaEvent.Builder()
//                .medJournalpostId(123456789)
//                .medTemaNytt("FOS")
//                .medJournalpostStatus("M")
//                .medHendelsestype("Mottatt")
//                .medMottakskanal("NAV_NO")
//                .build();
    }

    private EnrichedKafkaEvent getEntrichKafkaEvent(KafkaEvent event){
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(mockJournalpost(event));
        enrichedKafkaEvent.setIdenter(List.of(new Ident("1122334455", false, "AKTORID")));
        return enrichedKafkaEvent;
    }

    @Test
    public void test_when_journalfoering_catch_unknownException_send_to_manuell() throws Exception {
        KafkaEvent event = getTestEvent();
        doThrow(new ExternalServiceException("JournalpostApi", "Unknown error",500)).when(journalforingOperations).doAutomaticStuff(any());

        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent = journalforingOperationsKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals("123456789", jfr_manuell_outputTopic.readValue().getJournalpostId());
    }

    @Test
    public void test_journalpost_journalfoering_fail_send_to_keyValueStore() throws Exception {
        KafkaEvent event = getTestEvent();
        doThrow(new TemporarilyUnavailableException()).when(journalforingOperations).doAutomaticStuff(any());
        KeyValueStore<String, EnrichedKafkaEvent> store = testDriver.getKeyValueStore(JOURNALFOERING_OPERATION_STORE);
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent = store.get("Test123");
        assertEquals(kafkaEvent.getJournalpostId(), "123456789");
    }

    @Test
    public void test_journalpost_journalfoering_fail_multiple_time_send_to_manuell() throws Exception {
        KafkaEvent event = getTestEvent();
        doThrow(new TemporarilyUnavailableException()).when(journalforingOperations).doAutomaticStuff(any());
        KeyValueStore<String, EnrichedKafkaEvent> store = testDriver.getKeyValueStore(JOURNALFOERING_OPERATION_STORE);

        inputTopic.pipeInput("Test123", event);
        for (int i = 1; i < MAX_RETRY; i++) {
            EnrichedKafkaEvent kafkaEvent = store.get("Test123");
            assertEquals(kafkaEvent.getJournalpostId(), "123456789");
            testDriver.advanceWallClockTime(Duration.ofMinutes(30));
        }
        assertEquals(null,  store.get("Test123"));
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }

    @Test
    public void test_journalpost_journalfoering_succes_not_send_to_manuell_or_in_KV_store() {
        KafkaEvent event = getTestEvent();
        inputTopic.pipeInput("Test123", event);
        EnrichedKafkaEvent kafkaEvent = journalforingOperationsKVStore.get("Test123");
        assertNull(kafkaEvent);
        assert(jfr_manuell_outputTopic.isEmpty());
    }

    @Test
    public void test_journalpost_journafoering_fail_TUE_then_unknown_exception_send_to_manuell() throws Exception {
        KafkaEvent event = getTestEvent();
        doThrow(new TemporarilyUnavailableException()).when(journalforingOperations).doAutomaticStuff(any());
        inputTopic.pipeInput("Test123", event);
        // check store have journalpost stored after SUE
        EnrichedKafkaEvent kafkaEvent = journalforingOperationsKVStore.get("Test123");
        assertEquals(kafkaEvent.getJournalpostId(), "123456789");
        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        doThrow(new ExternalServiceException("JournalpostApi", "Unknown error",500)).when(journalforingOperations).doAutomaticStuff(any());
        testDriver.advanceWallClockTime(Duration.ofMinutes(30));
        kafkaEvent = journalforingOperationsKVStore.get("Test123");
        assertNull(kafkaEvent);
        assertEquals("123456789", jfr_manuell_outputTopic.readValue().getJournalpostId());
    }
}
