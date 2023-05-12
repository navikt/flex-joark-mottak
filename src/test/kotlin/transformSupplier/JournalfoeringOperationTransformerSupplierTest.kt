package transformSupplier

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import no.nav.helse.flex.operations.journalforing.JournalforingOperations
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockJournalpost
import java.time.Duration
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalfoeringOperationTransformerSupplierTest {
    private val journalforingOperations: JournalforingOperations = mockk(relaxed = true)

    private val MAX_RETRY = 48
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, KafkaEvent>
    private lateinit var outputTopic: TestOutputTopic<String, KafkaEvent>
    private lateinit var journalforingOperationsKVStore: KeyValueStore<String, EnrichedKafkaEvent>
    private val JOURNALFOERING_OPERATION_STORE = "journalfoeringoperations"
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

    private fun testTopology(): Topology {
        val journalfoeringOperationSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(JOURNALFOERING_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val journalOperationsTransformerSupplier = JournalOperationsTransformerSupplier(
            JOURNALFOERING_OPERATION_STORE,
            journalforingOperations
        )

        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(journalfoeringOperationSupplier)

        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))

        inputStream
            .mapValues { event: KafkaEvent -> getEntrichKafkaEvent(event) }
            .transform(journalOperationsTransformerSupplier, JOURNALFOERING_OPERATION_STORE)
            .filter { _, enrichedEvent -> enrichedEvent.isToManuell }
            .map { k: String, journalpostData: EnrichedKafkaEvent ->
                KeyValue.pair(k, journalpostData.kafkaEvent)
            }
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde))

        return streamsBuilder.build()
    }

    private val testEvent: KafkaEvent
        get() = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")

    private fun getEntrichKafkaEvent(event: KafkaEvent): EnrichedKafkaEvent {
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost(event)
        enrichedKafkaEvent.identer = listOf(Ident("AKTORID", "1122334455"))
        return enrichedKafkaEvent
    }

    @BeforeAll
    fun setup() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "mapping-stream-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"

        testDriver = TopologyTestDriver(testTopology(), props)
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, StringSerializer(), JfrKafkaSerializer())
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC,
            StringDeserializer(),
            JfrKafkaDeserializer(
                KafkaEvent::class.java
            )
        )
        journalforingOperationsKVStore = testDriver.getKeyValueStore(JOURNALFOERING_OPERATION_STORE)
    }

    @BeforeEach
    fun cleanUp() {
        journalforingOperationsKVStore.all().forEachRemaining {
            journalforingOperationsKVStore.delete(it.key)
        }
    }

    @Test
    fun test_when_journalfoering_catch_unknownException_send_to_manuell() {
        every { journalforingOperations.doAutomaticStuff(any()) } throws ExternalServiceException("Unknown error", 500)

        inputTopic.pipeInput("Test4", testEvent)
        val kafkaEvent = journalforingOperationsKVStore["Test4"]

        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }

    @Test
    fun test_journalpost_journalfoering_fail_send_to_keyValueStore() {
        every { journalforingOperations.doAutomaticStuff(any()) } throws TemporarilyUnavailableException()

        val store = testDriver.getKeyValueStore<String, EnrichedKafkaEvent>(JOURNALFOERING_OPERATION_STORE)
        inputTopic.pipeInput("Test1", testEvent)
        val kafkaEvent = store["Test1"]

        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_journalpost_journalfoering_fail_multiple_time_send_to_manuell() {
        every { journalforingOperations.doAutomaticStuff(any()) } throws TemporarilyUnavailableException()

        val store = testDriver.getKeyValueStore<String, EnrichedKafkaEvent>(JOURNALFOERING_OPERATION_STORE)
        inputTopic.pipeInput("Test2", testEvent)

        for (i in 1 until MAX_RETRY) {
            val kafkaEvent = store["Test2"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        }
        assertEquals(null, store["Test2"])
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }

    @Test
    fun test_journalpost_journalfoering_succes_not_send_to_manuell_or_in_KV_store() {
        every { journalforingOperations.doAutomaticStuff(any()) } answers { callOriginal() }
        inputTopic.pipeInput("Test123", testEvent)

        val kafkaEvent = journalforingOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertTrue(outputTopic.isEmpty)
    }

    @Test
    fun test_journalpost_journafoering_fail_TUE_then_unknown_exception_send_to_manuell() {
        every { journalforingOperations.doAutomaticStuff(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test3", testEvent)

        // check store have journalpost stored after SUE
        var kafkaEvent = journalforingOperationsKVStore["Test3"]
        assertEquals(kafkaEvent.journalpostId, "123456789")

        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        every { journalforingOperations.doAutomaticStuff(any()) } throws ExternalServiceException("Unknown error", 500)
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = journalforingOperationsKVStore["Test3"]

        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }
}
