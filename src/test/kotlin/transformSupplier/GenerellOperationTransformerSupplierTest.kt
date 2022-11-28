package transformSupplier

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import no.nav.helse.flex.operations.generell.GenerellOperations
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
import util.TestUtils.mockJournalpost
import java.time.Duration
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerellOperationTransformerSupplierTest {
    private var generellOperations: GenerellOperations = mockk(relaxed = true)

    private val MAX_RETRY = 48
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, KafkaEvent>
    private lateinit var outputTopic: TestOutputTopic<String, KafkaEvent>
    private lateinit var generellOperationsKVStore: KeyValueStore<String, EnrichedKafkaEvent>
    private val GENERELL_OPERATION_STORE = "generelloperations"
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
        val generellOperationSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(GENERELL_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val generellOperationsTransformerSupplier = GenerellOperationsTransformerSupplier(
            GENERELL_OPERATION_STORE,
            generellOperations
        )

        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(generellOperationSupplier)

        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))

        inputStream
            .mapValues { event: KafkaEvent -> getEntrichKafkaEvent(event) }
            .transform(generellOperationsTransformerSupplier, GENERELL_OPERATION_STORE)
            .filter { _, enrichedEvent: EnrichedKafkaEvent -> enrichedEvent.isToManuell }
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
            OUTPUT_TOPIC, StringDeserializer(),
            JfrKafkaDeserializer(
                KafkaEvent::class.java
            )
        )
        generellOperationsKVStore = testDriver.getKeyValueStore(GENERELL_OPERATION_STORE)
    }

    @BeforeEach
    fun cleanUp() {
        generellOperationsKVStore.all().forEachRemaining {
            generellOperationsKVStore.delete(it.key)
        }
    }

    @Test
    fun test_journalpost_fail_send_to_keyValueStore() {
        every { generellOperations.executeProcess(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test1", testEvent)
        val kafkaEvent = generellOperationsKVStore["Test1"]

        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_catch_unknownException_send_to_manuell() {
        every { generellOperations.executeProcess(any()) } throws Exception("Unknown Exception")

        inputTopic.pipeInput("Test2", testEvent)
        val kafkaEvent = generellOperationsKVStore["Test2"]

        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }

    @Test
    fun test_journalpost_fail_multiple_time_send_to_manuell() {
        every { generellOperations.executeProcess(any()) } throws TemporarilyUnavailableException()

        val event = testEvent
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost(event)
        enrichedKafkaEvent.identer = listOf(Ident("AKTORID", "1122334455"))

        inputTopic.pipeInput("Test3", event)
        for (i in 1 until MAX_RETRY) {
            val kafkaEvent = generellOperationsKVStore["Test3"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        }

        assertNull(generellOperationsKVStore["Test3"])
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }

    @Test
    fun test_journalpost_fail_then_unknown_exception_send_to_manuell() {
        every { generellOperations.executeProcess(any()) } throws TemporarilyUnavailableException()

        inputTopic.pipeInput("Test4", testEvent)

        // check store have journalpost stored after SUE
        var kafkaEvent = generellOperationsKVStore["Test4"]
        assertEquals(kafkaEvent.journalpostId, "123456789")

        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        every { generellOperations.executeProcess(any()) } throws Exception("Ukjent feil")

        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = generellOperationsKVStore["Test4"]

        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().getJournalpostId())
    }
}
