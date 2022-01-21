package transformSupplier

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.JfrTopologies
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost.Bruker
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
@PrepareForTest(JfrTopologies::class, GenerellOperationsTransformerSupplier::class, GenerellOperations::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class GenerellOperationTransformerSupplierTest {
    private val MAX_RETRY = 5
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, KafkaEvent>
    private lateinit var outputTopic: TestOutputTopic<String, KafkaEvent>
    private lateinit var generellOperations: GenerellOperations
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

    @Before
    fun setup() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "mapping-stream-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        generellOperations = PowerMockito.mock(GenerellOperations::class.java)
        PowerMockito.whenNew(GenerellOperations::class.java).withNoArguments().thenReturn(generellOperations)
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

    private fun testTopology(): Topology {
        val generellOperationSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(GENERELL_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val generellOperationsTransformerSupplier = GenerellOperationsTransformerSupplier(GENERELL_OPERATION_STORE)
        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(generellOperationSupplier)
        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))
        val enrichedKafkaEvent = inputStream.mapValues { event: KafkaEvent -> getEntrichKafkaEvent(event) }
        enrichedKafkaEvent
            .transform(generellOperationsTransformerSupplier, GENERELL_OPERATION_STORE)
            .filter { _, enrichedEvent: EnrichedKafkaEvent -> enrichedEvent.isToManuell }
            .map { k: String, journalpostData: EnrichedKafkaEvent ->
                KeyValue.pair(k, journalpostData.kafkaEvent)
            }
            .to(OUTPUT_TOPIC, Produced.with(Serdes.String(), kafkaEventSerde))
        return streamsBuilder.build()
    }

    private fun mockJournalpost(event: KafkaEvent): Journalpost {
        val mockBruker: Bruker
        mockBruker = Bruker("1234", "FNR")
        val mockJournalpost = Journalpost()
        mockJournalpost.setTittel("Test Journalpost")
        mockJournalpost.journalpostId = event.journalpostId
        mockJournalpost.journalforendeEnhet = "1111"
        mockJournalpost.dokumenter = listOf(Dokument("NAV 08-36.05", "DokTittel", "123"))
        mockJournalpost.bruker = mockBruker
        mockJournalpost.journalstatus = event.journalpostStatus
        mockJournalpost.tema = event.temaNytt
        return mockJournalpost
    }

    private val testEvent: KafkaEvent
        get() = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")

    private fun getEntrichKafkaEvent(event: KafkaEvent): EnrichedKafkaEvent {
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost(event)
        enrichedKafkaEvent.setIdenter(listOf(Ident("1122334455", false, "AKTORID")))
        return enrichedKafkaEvent
    }

    @Test
    fun test_journalpost_fail_send_to_keyValueStore() {
        val event = testEvent
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(generellOperations)
            .executeProcess(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = generellOperationsKVStore["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_catch_unknownException_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(Exception("Unknown Exception")).`when`(generellOperations)
            .executeProcess(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = generellOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_fail_multiple_time_send_to_manuell() {
        val event = testEvent
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost(event)
        enrichedKafkaEvent.setIdenter(listOf(Ident("1122334455", false, "AKTORID")))
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(generellOperations)
            .executeProcess(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)
        for (i in 1 until MAX_RETRY) {
            val kafkaEvent = generellOperationsKVStore["Test123"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        }
        assertNull(generellOperationsKVStore["Test123"])
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_fail_then_unknown_exception_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(generellOperations)
            .executeProcess(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)

        // check store have journalpost stored after SUE
        var kafkaEvent = generellOperationsKVStore["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")

        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        PowerMockito.doThrow(Exception("Ukjent feil")).`when`(generellOperations).executeProcess(ArgumentMatchers.any())
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = generellOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }
}
