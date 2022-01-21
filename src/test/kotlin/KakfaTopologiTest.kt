import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import no.nav.helse.flex.Environment
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrTopologies
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier
import no.nav.helse.flex.operations.Feilregistrer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Transformer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner
import util.TestUtils.mockJournalpost
import util.TestUtils.mockJournalpostEvent
import java.util.*

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("no.nav.helse.flex.Environment")
@PrepareForTest(
    JfrTopologies::class,
    GenerellOperationsTransformerSupplier::class,
    JournalOperationsTransformerSupplier::class,
    EventEnricherTransformerSupplier::class
)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class KakfaTopologiTest {
    val INPUT_TOPIC = "source-topic"
    val OUTPUT_TOPIC = "output-topic"
    val MOCK_SCHEMA_REGISTRY_URL = "mock://SCHEMA_REGISTRY_URL"

    lateinit var testDriver: TopologyTestDriver
    lateinit var inputTopic: TestInputTopic<String, GenericRecord>
    lateinit var outputTopic: TestOutputTopic<String, EnrichedKafkaEvent>

    val mockEnrichTransformer: Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mock()
    val mockGenerellTransformer: Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mock()
    val mockJournalfoeringTransformer: Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mock()

    val eventEnricherTransformerSupplier: EventEnricherTransformerSupplier = mock()
    val generellOperationsTransformerSupplier: GenerellOperationsTransformerSupplier = mock()
    val journalOperationsTransformerSupplier: JournalOperationsTransformerSupplier = mock()
    val feilregistrer: Feilregistrer = mock()

    @Before
    fun setup() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "mapping-stream-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"

        PowerMockito.whenNew(EventEnricherTransformerSupplier::class.java).withAnyArguments()
            .thenReturn(eventEnricherTransformerSupplier)
        `when`(eventEnricherTransformerSupplier.get()).thenReturn(mockEnrichTransformer)

        PowerMockito.whenNew(GenerellOperationsTransformerSupplier::class.java).withAnyArguments()
            .thenReturn(generellOperationsTransformerSupplier)
        `when`(generellOperationsTransformerSupplier.get()).thenReturn(mockGenerellTransformer)

        PowerMockito.whenNew(JournalOperationsTransformerSupplier::class.java).withAnyArguments()
            .thenReturn(journalOperationsTransformerSupplier)
        `when`(journalOperationsTransformerSupplier.get()).thenReturn(mockJournalfoeringTransformer)

        PowerMockito.whenNew(Feilregistrer::class.java).withAnyArguments().thenReturn(feilregistrer)

        PowerMockito.spy(Environment::class.java)
        PowerMockito.doReturn("automatiskSkjema.json")
            .`when`(Environment::class.java, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING")
        PowerMockito.doReturn(
            mapOf(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to MOCK_SCHEMA_REGISTRY_URL,
                SchemaRegistryClientConfig.USER_INFO_CONFIG to "username:password",
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO"
            )
        ).`when`(Environment::class.java, "getKafkaSerdeConfig")

        val jfrTopologies = JfrTopologies(INPUT_TOPIC, OUTPUT_TOPIC)
        val topology = jfrTopologies.jfrTopologi
        testDriver = TopologyTestDriver(topology, props)

        val valueGenericAvroSerde: Serde<GenericRecord> = GenericAvroSerde()
        val serdeConfig = Environment.getKafkaSerdeConfig()
        valueGenericAvroSerde.configure(serdeConfig, false)

        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC,
            StringSerializer(),
            valueGenericAvroSerde.serializer()
        )
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC,
            StringDeserializer(),
            JfrKafkaDeserializer(EnrichedKafkaEvent::class.java)
        )
    }

    @Test
    fun test_skjema_is_not_automatic_expect_to_manuell() {
        val mockedJournalpostEvent = mockJournalpostEvent("SYK")
        val event = Gson().fromJson(mockedJournalpostEvent.toString(), KafkaEvent::class.java)
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "ABC", "SYK", "M")

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        inputTopic.pipeInput("Test123", mockedJournalpostEvent)

        assertEquals(1, outputTopic.queueSize)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_toManuell_flag_from_enricher_is_true_expect_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToManuell = true

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue.pair("Test123", enrichedKafkaEvent))
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(1, outputTopic.queueSize)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_toIgnore_flag_from_enricher_is_true_expect_not_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToIgnore = true

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(0, outputTopic.queueSize)
    }

    @Test
    fun test_toIgnore_and_toManuell_flag_from_enricher_is_true_expect_not_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToIgnore = true
        enrichedKafkaEvent.isToManuell = true

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(0, outputTopic.queueSize)
    }

    @Test
    fun test_toManuell_flag_is_true_expect_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val generellKafkaEvent = EnrichedKafkaEvent(event)
        generellKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        `when`(mockGenerellTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", generellKafkaEvent.withSetToManuell(true)))
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(1, outputTopic.queueSize)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_toManuell_flag_from_journafoering_is_true_expect_to_manuall() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val postJournalfoeringKafkaEvent = EnrichedKafkaEvent(event)
        postJournalfoeringKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        `when`(mockGenerellTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        `when`(mockJournalfoeringTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(
            KeyValue("Test123", postJournalfoeringKafkaEvent.withSetToManuell(true))
        )
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(1, outputTopic.queueSize)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_success_journafoering_expect_not_to_manuall() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val postJournalfoeringKafkaEvent = EnrichedKafkaEvent(event)
        postJournalfoeringKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")

        `when`(mockEnrichTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        `when`(mockGenerellTransformer.transform(Mockito.anyString(), Mockito.any()))
            .thenReturn(KeyValue("Test123", enrichedKafkaEvent))
        `when`(mockJournalfoeringTransformer.transform(Mockito.anyString(), Mockito.any())).thenReturn(
            KeyValue("Test123", postJournalfoeringKafkaEvent)
        )
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        assertEquals(0, outputTopic.queueSize)
    }

    @After
    fun tearDown() {
        testDriver.close()
    }
}
