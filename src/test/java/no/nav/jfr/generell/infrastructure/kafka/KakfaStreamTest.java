package no.nav.jfr.generell.infrastructure.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier;
import no.nav.jfr.generell.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier;
import no.nav.jfr.generell.operations.Feilregistrer;
import no.nav.jfr.generell.operations.eventenricher.EventEnricher;
import no.nav.jfr.generell.operations.generell.GenerellOperations;
import no.nav.jfr.generell.operations.journalforing.JournalforingOperations;
import operations.TestUtils;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

@Ignore
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.jfr.generell.Environment"})
@PrepareForTest({JfrTopologies.class, GenerellOperationsTransformerSupplier.class, JournalOperationsTransformerSupplier.class, EventEnricherTransformerSupplier.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class KakfaStreamTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<String, GenericRecord> inputTopic;
    private TestOutputTopic<String, KafkaEvent> jfr_manuell_outputTopic;
    private EventEnricher eventEnricher;
    private GenerellOperations generellOperations;
    private JournalforingOperations journalforingOperations;
    private Feilregistrer feilregistrer;

    private static final String SCHEMA_REGISTRY_SCOPE = KakfaStreamTest.class.getName();
    private static final String MOCK_SCHEMA_REGISTRY_URL = "mock://" + SCHEMA_REGISTRY_SCOPE;

    @Before
    public void setup() throws Exception {
        final Properties props = new Properties();
        String INPUT_TOPIC = "source-topic";
        String OUTPUT_TOPIC = "output-topic";
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        eventEnricher = mock(EventEnricher.class);
        PowerMockito.whenNew(EventEnricher.class).withNoArguments().thenReturn(eventEnricher);

        generellOperations = mock(GenerellOperations.class);
        PowerMockito.whenNew(GenerellOperations.class).withNoArguments().thenReturn(generellOperations);

        journalforingOperations = mock(JournalforingOperations.class);
        PowerMockito.whenNew(JournalforingOperations.class).withNoArguments().thenReturn(journalforingOperations);

        feilregistrer = mock(Feilregistrer.class);
        PowerMockito.whenNew(Feilregistrer.class).withAnyArguments().thenReturn(feilregistrer);

        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING");
        doReturn(Map.of(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_SCHEMA_REGISTRY_URL,
                SchemaRegistryClientConfig.USER_INFO_CONFIG, "username:password",
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")).when(Environment.class, "getKafkaSerdeConfig");

        final JfrTopologies jfrTopologies = new JfrTopologies(INPUT_TOPIC, OUTPUT_TOPIC);
        final Topology topology = jfrTopologies.getJfrTopologi();

        testDriver = new TopologyTestDriver(topology, props);
        final Map<String, String> serdeConfig = Environment.getKafkaSerdeConfig();
        final Serde<GenericRecord> valueGenericAvroSerde = new GenericAvroSerde();
        valueGenericAvroSerde.configure(serdeConfig, false);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, new StringSerializer(), valueGenericAvroSerde.serializer());
        jfr_manuell_outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, new StringDeserializer(), new JfrKafkaDeserializer<>(KafkaEvent.class));
    }

//    @Test
//    public void test_journalpost_that_does_not_have_infotrygt_tema_will_not_go_to_manuell_topic(){
//        GenericRecord journalHendelse = TestUtils.mockJournalpostEvent("BAR");
//        inputTopic.pipeInput("Test123", journalHendelse);
//        assert(jfr_manuell_outputTopic.isEmpty());
//    }

    @Test
    public void test_journalpost_skjema_to_manuell_topic() throws Exception{
//        doAnswer((Answer<Void>) invocationOnMock -> {
//            EnrichedKafkaEvent eke = invocationOnMock.getArgument(0);
//            eke.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 21-04.05", "KON", "M"));
//            return null;
//        }).when(eventEnricher).createEnrichedKafkaEvent(Mockito.any());

        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("GEN"));
        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
        assert(jfr_manuell_outputTopic.readValue().getJournalpostId().equals("123456789"));
    }
//
//    @Test
//    public void test_journalpost_skjema_not_to_manuell_topic() throws Exception{
//        doAnswer((Answer<Void>) invocationOnMock -> {
//            EnrichedKafkaEvent eke = invocationOnMock.getArgument(0);
//            eke.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 34-00.08", "KON", "M"));
//            return null;
//        }).when(eventEnricher).createEnrichedKafkaEvent(Mockito.any());
//        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("KON"));
//
//        assertEquals(0, jfr_manuell_outputTopic.getQueueSize());
//    }
//
//    @Test
//    public void test_enricher_flag_to_manuell()throws Exception{
//        doAnswer((Answer<Void>) invocationOnMock -> {
//            EnrichedKafkaEvent eke = invocationOnMock.getArgument(0);
//            eke.setJournalpost(TestUtils.mockJournalpost("123456789", "NAV 21-04.05", "KON", "M"));
//            return null;
//        }).when(eventEnricher).createEnrichedKafkaEvent(Mockito.any());
//        inputTopic.pipeInput("Test123", TestUtils.mockJournalpostEvent("KON"));
//        assertEquals(1, jfr_manuell_outputTopic.getQueueSize());
//    }

    @After
    public void tearDown() {
        testDriver.close();
    }
}
