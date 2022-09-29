package transformSupplier

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier
import no.nav.helse.flex.operations.eventenricher.EventEnricher
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnricherOperationTransformerSupplierTest {
    private val eventEnricher: EventEnricher = mockk(relaxed = true)

    private val MAX_RETRY = 5
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private val props = Properties().apply {
        set(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app")
        set(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    }
    private val ENRICHER_OPERATION_STORE = "enrichoperations"
    private val enhancedKafkaEventSerde = Serdes.serdeFrom(
        JfrKafkaSerializer(),
        JfrKafkaDeserializer(
            EnrichedKafkaEvent::class.java
        )
    )
    private val kafkaEventSerde = Serdes.serdeFrom(
        JfrKafkaSerializer(),
        JfrKafkaDeserializer(
            KafkaEvent::class.java
        )
    )

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, KafkaEvent>
    private lateinit var outputTopic: TestOutputTopic<String, KafkaEvent>
    private lateinit var enricherKVStore: KeyValueStore<String, EnrichedKafkaEvent>

    private fun testEnricherTopology(): Topology {
        val eventEnricherSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(ENRICHER_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val eventEnricherTransformerSupplier = EventEnricherTransformerSupplier(
            ENRICHER_OPERATION_STORE,
            eventEnricher
        )

        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(eventEnricherSupplier)

        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))

        inputStream
            .transform(eventEnricherTransformerSupplier, ENRICHER_OPERATION_STORE)
            .filter { _, enrichedEvent: EnrichedKafkaEvent -> enrichedEvent.isToManuell }
            .map { k: String, journalpostData: EnrichedKafkaEvent ->
                KeyValue.pair(k, journalpostData.kafkaEvent)
            }
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde))

        return streamsBuilder.build()
    }

    private val testEvent: KafkaEvent
        get() = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")

    @BeforeAll
    fun setup() {
        testDriver = TopologyTestDriver(testEnricherTopology(), props)
        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC, StringSerializer(), JfrKafkaSerializer()
        )
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC, StringDeserializer(), JfrKafkaDeserializer(KafkaEvent::class.java)
        )
        enricherKVStore = testDriver.getKeyValueStore(ENRICHER_OPERATION_STORE)
    }

    @BeforeEach
    fun cleanUp() {
        enricherKVStore.all().forEachRemaining {
            enricherKVStore.delete(it.key)
        }
    }

    @Test
    fun test_when_enricher_catch_unknownException_retry() {
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws Exception("Unknown Error")

        inputTopic.pipeInput("Test123", testEvent)
        val kafkaEvent = enricherKVStore["Test123"]

        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_journalpost_get_sent_to_enricher_fail_send_to_keyValueStore() {
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test123", testEvent)
        val enrichedKafkaEvent = enricherKVStore["Test123"]

        assertEquals("123456789", enrichedKafkaEvent.journalpostId)
    }

    @Test
    fun test_journalpost_enricher_fail_more_than_max_retry_send_to_manuell() {
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test123", testEvent)

        // kafkaEvent store i KV-store when SUE
        var kafkaEvent: EnrichedKafkaEvent?
        for (i in 1 until MAX_RETRY) {
            kafkaEvent = enricherKVStore["Test123"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        }

        // after max retry attempt - Journlapost should be send to manuell and store empty
        kafkaEvent = enricherKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }

    @Test
    fun test_journalpost_enricher_fail_TUE_then_unknown_exception_retry() {
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test123", testEvent)

        // check store have journalpost stored
        var kafkaEvent = enricherKVStore["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")

        // after 30 min schedule retry
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws Exception("Ukjent feil")
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = enricherKVStore["Test123"]

        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_journalpost_enricher_not_continiouProcess_if_SAF_status_is_J() {
        every { eventEnricher.createEnrichedKafkaEvent(any()) } throws InvalidJournalpostStatusException()

        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = enricherKVStore["Test123"]

        assertNull(kafkaEvent)
        assertEquals(0, outputTopic.queueSize)
    }
}
