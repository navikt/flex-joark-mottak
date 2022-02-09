import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.helse.flex.Environment
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrTopologies
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.OppgaveOperationsTransformerSupplier
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.operations.Feilregistrer
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Transformer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockJournalpost
import util.TestUtils.mockJournalpostEvent
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KakfaTopologiTest {
    val INPUT_TOPIC = "source-topic"
    val MOCK_SCHEMA_REGISTRY_URL = "mock://SCHEMA_REGISTRY_URL"

    lateinit var testDriver: TopologyTestDriver
    lateinit var inputTopic: TestInputTopic<String, GenericRecord>

    val mockEnrichTransformer: Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mockk(relaxed = true)
    val mockGenerellTransformer: Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mockk(relaxed = true)
    val mockJournalfoeringTransformer: Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mockk(relaxed = true)
    val mockOppgaveTransformer: Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> = mockk(relaxed = true)

    val eventEnricherTransformerSupplier: EventEnricherTransformerSupplier = mockk(relaxed = true)
    val generellOperationsTransformerSupplier: GenerellOperationsTransformerSupplier = mockk(relaxed = true)
    val journalOperationsTransformerSupplier: JournalOperationsTransformerSupplier = mockk(relaxed = true)
    val oppgaveOperationsTransformerSupplier: OppgaveOperationsTransformerSupplier = mockk(relaxed = true)
    val feilregistrer: Feilregistrer = mockk(relaxed = true)

    @BeforeAll
    fun setup() {
        every { eventEnricherTransformerSupplier.get() } returns mockEnrichTransformer
        every { generellOperationsTransformerSupplier.get() } returns mockGenerellTransformer
        every { journalOperationsTransformerSupplier.get() } returns mockJournalfoeringTransformer
        every { oppgaveOperationsTransformerSupplier.get() } returns mockOppgaveTransformer
        every { mockOppgaveTransformer.transform(any(), any()) } returns null

        mockkObject(Environment)
        every { Environment.getEnvVar("KAFKA_SCHEMA_REGISTRY") } returns MOCK_SCHEMA_REGISTRY_URL
        every { Environment.getEnvVar("KAFKA_SCHEMA_REGISTRY_USER") } returns "username"
        every { Environment.getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD") } returns "password"

        val jfrTopologies = JfrTopologies(
            inputTopic = INPUT_TOPIC,
            feilregistrer = feilregistrer,
            eventEnricherTransformerSupplier = eventEnricherTransformerSupplier,
            generellOperationsTransformerSupplier = generellOperationsTransformerSupplier,
            journalOperationsTransformerSupplier = journalOperationsTransformerSupplier,
            oppgaveOperationsTransformerSupplier = oppgaveOperationsTransformerSupplier,
        )
        val topology = jfrTopologies.jfrTopologi
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "mapping-stream-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        testDriver = TopologyTestDriver(topology, props)

        val valueGenericAvroSerde: Serde<GenericRecord> = GenericAvroSerde()
        val serdeConfig = mapOf(
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to MOCK_SCHEMA_REGISTRY_URL,
            SchemaRegistryClientConfig.USER_INFO_CONFIG to "username:password",
            SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO"
        )
        valueGenericAvroSerde.configure(serdeConfig, false)

        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC,
            StringSerializer(),
            valueGenericAvroSerde.serializer()
        )
    }

    @BeforeEach
    fun clear() {
        clearMocks(feilregistrer)
    }

    @Test
    fun test_skjema_is_not_automatic_expect_to_manuell() {
        val mockedJournalpostEvent = mockJournalpostEvent("SYK")
        val event: KafkaEvent = objectMapper.readValue(mockedJournalpostEvent.toString())
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "ABC", "SYK", "M")

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        inputTopic.pipeInput("Test123", mockedJournalpostEvent)

        verify { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_toManuell_flag_from_enricher_is_true_expect_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToManuell = true

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue.pair("Test123", enrichedKafkaEvent)
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_toIgnore_flag_from_enricher_is_true_expect_not_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToIgnore = true

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify(exactly = 0) { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_toIgnore_and_toManuell_flag_from_enricher_is_true_expect_not_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        enrichedKafkaEvent.isToIgnore = true
        enrichedKafkaEvent.isToManuell = true

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify(exactly = 0) { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_toManuell_flag_is_true_expect_to_manuell() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val generellKafkaEvent = EnrichedKafkaEvent(event)
        generellKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        every { mockGenerellTransformer.transform(any(), any()) } returns KeyValue("Test123", generellKafkaEvent.withSetToManuell(true))
        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_toManuell_flag_from_journafoering_is_true_expect_to_manuall() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val postJournalfoeringKafkaEvent = EnrichedKafkaEvent(event)
        postJournalfoeringKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        every { mockGenerellTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        every { mockJournalfoeringTransformer.transform(any(), any()) } returns KeyValue("Test123", postJournalfoeringKafkaEvent.withSetToManuell(true))

        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify { feilregistrer.feilregistrerOppgave(any()) }
    }

    @Test
    fun test_success_journafoering_expect_not_to_manuall() {
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        val postJournalfoeringKafkaEvent = EnrichedKafkaEvent(event)
        postJournalfoeringKafkaEvent.journalpost = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")

        every { mockEnrichTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        every { mockGenerellTransformer.transform(any(), any()) } returns KeyValue("Test123", enrichedKafkaEvent)
        every { mockJournalfoeringTransformer.transform(any(), any()) } returns KeyValue("Test123", postJournalfoeringKafkaEvent)

        inputTopic.pipeInput("Test123", mockJournalpostEvent("SYK"))

        verify(exactly = 0) { feilregistrer.feilregistrerOppgave(any()) }
    }

    @AfterAll
    fun tearDown() {
        testDriver.close()
    }
}
