package no.nav.jfr.generell.infrastructure.kafka;

import com.google.gson.Gson;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier;
import no.nav.jfr.generell.operations.Feilregistrer;
import operations.TestUtils;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Transformer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.jfr.infotrygd.Environment"})
@PrepareForTest({JfrTopologies.class, GenerellOperationsTransformerSupplier.class, JournalOperationsTransformerSupplier.class, EventEnricherTransformerSupplier.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@Ignore
public class KakfaTopologiTest {
    private static final String SCHEMA_REGISTRY_SCOPE = KakfaStreamTest.class.getName();
    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, GenericRecord> inputTopic;
    private TestOutputTopic<String, KafkaEvent> jfr_manuell_outputTopic;
    Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> mockEnrichTransformer = mock(Transformer.class);
    Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> mockInfotrygdTransformer = mock(Transformer.class);
    Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> mockJournalfoeringTransformer = mock(Transformer.class);

    @Before
    public void setup() throws Exception {
        final Properties props = new Properties();

        String INPUT_TOPIC = "source-topic";
        String OUTPUT_TOPIC = "output-topic";
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        EventEnricherTransformerSupplier eventEnricherTransformerSupplier = mock(EventEnricherTransformerSupplier.class);
        PowerMockito.whenNew(EventEnricherTransformerSupplier.class).withAnyArguments().thenReturn(eventEnricherTransformerSupplier);
        when(eventEnricherTransformerSupplier.get()).thenReturn(mockEnrichTransformer);

        GenerellOperationsTransformerSupplier infotrygdOperationTransformerSupplier = mock(GenerellOperationsTransformerSupplier.class);
        PowerMockito.whenNew(GenerellOperationsTransformerSupplier.class).withAnyArguments().thenReturn(infotrygdOperationTransformerSupplier);
        when(infotrygdOperationTransformerSupplier.get()).thenReturn(mockInfotrygdTransformer);

        JournalOperationsTransformerSupplier journalOperationsTransformerSupplier = mock(JournalOperationsTransformerSupplier.class);
        PowerMockito.whenNew(JournalOperationsTransformerSupplier.class).withAnyArguments().thenReturn(journalOperationsTransformerSupplier);
        when(journalOperationsTransformerSupplier.get()).thenReturn(mockJournalfoeringTransformer);

        Feilregistrer feilregistrer = mock(Feilregistrer.class);
        PowerMockito.whenNew(Feilregistrer.class).withAnyArguments().thenReturn(feilregistrer);

        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING_STK");
        doReturn(Map.of(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL,
                SchemaRegistryClientConfig.USER_INFO_CONFIG, "username:password",
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")).when(Environment.class, "getKafkaSerdeConfig");

        final JfrTopologies jfrTopologies = new JfrTopologies(INPUT_TOPIC, OUTPUT_TOPIC);
        final Topology topology = jfrTopologies.getJfrTopologi();

        testDriver = new TopologyTestDriver(topology, props);
        final Serde<GenericRecord> valueGenericAvroSerde = new GenericAvroSerde();
        final Map<String, String> serdeConfig = Environment.getKafkaSerdeConfig();
        valueGenericAvroSerde.configure(serdeConfig, false);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), valueGenericAvroSerde.serializer());
        jfr_manuell_outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new JfrKafkaDeserializer<>(KafkaEvent.class));
    }

    @Test
    public void test_skjema_is_not_automatic_expect_to_manuell() throws Exception {
        final GenericRecord mockedJournalpostEvent = TestUtils.mockJournalpostEvent("GRU");
        final KafkaEvent event = new Gson().fromJson(mockedJournalpostEvent.toString(), KafkaEvent.class);
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "VANL", "GRU", "M"));
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        inputTopic.pipeInput("Test123", mockedJournalpostEvent);

        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }

    @Test
    public void test_toManuell_flag_from_enricher_is_true_expect_to_manuell(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        enrichedKafkaEvent.setToManuell(true);
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }

    @Test
    public void test_toIgnore_flag_from_enricher_is_true_expect_not_to_manuell(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        enrichedKafkaEvent.setToIgnore(true);
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(0, jfr_manuell_outputTopic.getQueueSize());
    }

    @Test
    public void test_toIgnore_and_toManuell_flag_from_enricher_is_true_expect_not_to_manuell(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        enrichedKafkaEvent.setToIgnore(true);
        enrichedKafkaEvent.setToManuell(true);
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(0, jfr_manuell_outputTopic.getQueueSize());
    }

    @Test
    public void test_toManuell_flag_from_infotrygd_is_true_expect_to_manuell(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        EnrichedKafkaEvent postInfotrygKafkaEvent = new EnrichedKafkaEvent(event);
        postInfotrygKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        when(mockInfotrygdTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", postInfotrygKafkaEvent.withSetToManuell(true)));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }

    @Test
    public void test_toManuell_flag_from_journafoering_is_true_expect_to_manuall(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        EnrichedKafkaEvent postJournalfoeringKafkaEvent = new EnrichedKafkaEvent(event);
        postJournalfoeringKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        when(mockInfotrygdTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        when(mockJournalfoeringTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", postJournalfoeringKafkaEvent.withSetToManuell(true)));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }

    @Test
    public void test_success_journafoering_expect_not_to_manuall(){
        KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        enrichedKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        EnrichedKafkaEvent postJournalfoeringKafkaEvent = new EnrichedKafkaEvent(event);
        postJournalfoeringKafkaEvent.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M"));
        when(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        when(mockInfotrygdTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", enrichedKafkaEvent));
        when(mockJournalfoeringTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(new KeyValue<>("Test123", postJournalfoeringKafkaEvent));
        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GRU"));
        assertEquals(0, jfr_manuell_outputTopic.getQueueSize());
    }

    @After
    public void tearDown() {
        testDriver.close();
    }
}
