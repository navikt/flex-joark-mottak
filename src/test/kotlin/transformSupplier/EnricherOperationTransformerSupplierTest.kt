package transformSupplier

import junit.framework.TestCase.assertNull
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.JfrTopologies
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.time.Duration
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(JfrTopologies::class, EventEnricherTransformerSupplier::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class EnricherOperationTransformerSupplierTest {
    private val MAX_RETRY = 5
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private val props = Properties().apply {
        set(StreamsConfig.APPLICATION_ID_CONFIG, "mapping-stream-app")
        set(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    }
    private val ENRICHER_OPERATION_STORE = "enrichoperations"
    private val eventEnricher = PowerMockito.mock(EventEnricher::class.java)
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

    @Before
    fun setup() {
        PowerMockito.whenNew(EventEnricher::class.java).withNoArguments().thenReturn(eventEnricher)
        testDriver = TopologyTestDriver(testEnricherTopology(), props)
        inputTopic = testDriver.createInputTopic(
            INPUT_TOPIC, StringSerializer(), JfrKafkaSerializer()
        )
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC, StringDeserializer(), JfrKafkaDeserializer(KafkaEvent::class.java)
        )
        enricherKVStore = testDriver.getKeyValueStore(ENRICHER_OPERATION_STORE)
    }

    private fun testEnricherTopology(): Topology {
        val eventEnricherSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(ENRICHER_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val eventEnricherTransformerSupplier = EventEnricherTransformerSupplier(ENRICHER_OPERATION_STORE)
        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(eventEnricherSupplier)
        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))
        val enrichedKafkaEvent = inputStream.transform(eventEnricherTransformerSupplier, ENRICHER_OPERATION_STORE)
        enrichedKafkaEvent
            .filter { _, enrichedEvent: EnrichedKafkaEvent -> enrichedEvent.isToManuell }
            .map { k: String, journalpostData: EnrichedKafkaEvent ->
                KeyValue.pair(
                    k,
                    journalpostData.kafkaEvent
                )
            }
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde))
        return streamsBuilder.build()
    }

    private val testEvent: KafkaEvent
        get() = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")

    @Test
    fun test_when_enricher_catch_unknownException_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(Exception("Unknown Error")).`when`(eventEnricher).createEnrichedKafkaEvent(
            ArgumentMatchers.any(
                EnrichedKafkaEvent::class.java
            )
        )
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = enricherKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_get_sent_to_enricher_fail_send_to_keyValueStore() {
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(eventEnricher).createEnrichedKafkaEvent(
            ArgumentMatchers.any(
                EnrichedKafkaEvent::class.java
            )
        )
        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        val enrichedKafkaEvent = enricherKVStore["Test123"]
        assertEquals("123456789", enrichedKafkaEvent.journalpostId)
    }

    @Test
    fun test_journalpost_enricher_fail_more_than_max_retry_send_to_manuell() {
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(eventEnricher).createEnrichedKafkaEvent(
            ArgumentMatchers.any(
                EnrichedKafkaEvent::class.java
            )
        )

        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        var kafkaEvent: EnrichedKafkaEvent?
        for (i in 1 until MAX_RETRY) {
            kafkaEvent = enricherKVStore["Test123"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
            // kafkaEvent store i KV-store when SUE
        }

        // after max retry attempt - Journlapost should be send to manuell and store empty
        kafkaEvent = enricherKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_enricher_fail_SUE_then_unknown_exception_send_to_manuell() {
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(eventEnricher).createEnrichedKafkaEvent(
            ArgumentMatchers.any(
                EnrichedKafkaEvent::class.java
            )
        )

        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        // check store have journalpost staored after SUE
        var kafkaEvent = enricherKVStore["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")
        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        PowerMockito.doThrow(Exception("Ukjent feil")).`when`(eventEnricher).createEnrichedKafkaEvent(
            ArgumentMatchers.any(
                EnrichedKafkaEvent::class.java
            )
        )
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = enricherKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_enricher_not_continiouProcess_if_SAF_status_is_J() {
        PowerMockito.doThrow(InvalidJournalpostStatusException()).`when`(eventEnricher)
            .createEnrichedKafkaEvent(ArgumentMatchers.any())

        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = enricherKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals(0, outputTopic.queueSize)
    }
}
