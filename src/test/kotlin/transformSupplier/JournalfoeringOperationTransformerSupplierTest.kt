package transformSupplier

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaDeserializer
import no.nav.helse.flex.infrastructure.kafka.JfrKafkaSerializer
import no.nav.helse.flex.infrastructure.kafka.JfrTopologies
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost.Bruker
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
@PrepareForTest(JfrTopologies::class, JournalOperationsTransformerSupplier::class, JournalforingOperations::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class JournalfoeringOperationTransformerSupplierTest {
    private val MAX_RETRY = 5
    private val INPUT_TOPIC = "source-topic"
    private val OUTPUT_TOPIC = "output-topic"
    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, KafkaEvent>
    private lateinit var outputTopic: TestOutputTopic<String, KafkaEvent>
    private lateinit var journalforingOperations: JournalforingOperations
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

    @Before
    fun setup() {
        val props = Properties()
        props[StreamsConfig.APPLICATION_ID_CONFIG] = "mapping-stream-app"
        props[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        journalforingOperations = PowerMockito.mock(JournalforingOperations::class.java)
        PowerMockito.whenNew(JournalforingOperations::class.java).withNoArguments().thenReturn(journalforingOperations)
        testDriver = TopologyTestDriver(testTopology(), props)
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, StringSerializer(), JfrKafkaSerializer())
        outputTopic = testDriver.createOutputTopic(
            OUTPUT_TOPIC, StringDeserializer(),
            JfrKafkaDeserializer(
                KafkaEvent::class.java
            )
        )
        journalforingOperationsKVStore = testDriver.getKeyValueStore(JOURNALFOERING_OPERATION_STORE)
    }

    private fun testTopology(): Topology {
        val journalfoeringOperationSupplier = Stores.keyValueStoreBuilder(
            Stores.inMemoryKeyValueStore(JOURNALFOERING_OPERATION_STORE),
            Serdes.String(),
            enhancedKafkaEventSerde
        )
        val journalOperationsTransformerSupplier = JournalOperationsTransformerSupplier(JOURNALFOERING_OPERATION_STORE)
        val streamsBuilder = StreamsBuilder()
        streamsBuilder.addStateStore(journalfoeringOperationSupplier)
        val inputStream = streamsBuilder.stream(INPUT_TOPIC, Consumed.with(Serdes.String(), kafkaEventSerde))
        val enrichedKafkaEvent = inputStream.mapValues { event: KafkaEvent -> getEntrichKafkaEvent(event) }
        val postJournalfoeringKafkaEvent = enrichedKafkaEvent
            .transform(journalOperationsTransformerSupplier, JOURNALFOERING_OPERATION_STORE)
        postJournalfoeringKafkaEvent
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
        get() = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "FOS", "NAV_NO")

    private fun getEntrichKafkaEvent(event: KafkaEvent): EnrichedKafkaEvent {
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        enrichedKafkaEvent.journalpost = mockJournalpost(event)
        enrichedKafkaEvent.setIdenter(listOf(Ident("1122334455", false, "AKTORID")))
        return enrichedKafkaEvent
    }

    @Test
    fun test_when_journalfoering_catch_unknownException_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(ExternalServiceException("JournalpostApi", "Unknown error", 500))
            .`when`(journalforingOperations).doAutomaticStuff(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = journalforingOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_journalfoering_fail_send_to_keyValueStore() {
        val event = testEvent
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(journalforingOperations)
            .doAutomaticStuff(ArgumentMatchers.any())
        val store = testDriver.getKeyValueStore<String, EnrichedKafkaEvent>(JOURNALFOERING_OPERATION_STORE)
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = store["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")
    }

    @Test
    fun test_journalpost_journalfoering_fail_multiple_time_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(journalforingOperations)
            .doAutomaticStuff(ArgumentMatchers.any())
        val store = testDriver.getKeyValueStore<String, EnrichedKafkaEvent>(JOURNALFOERING_OPERATION_STORE)
        inputTopic.pipeInput("Test123", event)
        for (i in 1 until MAX_RETRY) {
            val kafkaEvent = store["Test123"]
            assertEquals(kafkaEvent.journalpostId, "123456789")
            testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        }
        assertEquals(null, store["Test123"])
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }

    @Test
    fun test_journalpost_journalfoering_succes_not_send_to_manuell_or_in_KV_store() {
        val event = testEvent
        inputTopic.pipeInput("Test123", event)
        val kafkaEvent = journalforingOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertTrue(outputTopic.isEmpty)
    }

    @Test
    fun test_journalpost_journafoering_fail_TUE_then_unknown_exception_send_to_manuell() {
        val event = testEvent
        PowerMockito.doThrow(TemporarilyUnavailableException()).`when`(journalforingOperations)
            .doAutomaticStuff(ArgumentMatchers.any())
        inputTopic.pipeInput("Test123", event)

        // check store have journalpost stored after SUE
        var kafkaEvent = journalforingOperationsKVStore["Test123"]
        assertEquals(kafkaEvent.journalpostId, "123456789")

        // after 30 min schedule retry - and throws UE Journlapost should send to manuell and store empty
        PowerMockito.doThrow(ExternalServiceException("JournalpostApi", "Unknown error", 500))
            .`when`(journalforingOperations).doAutomaticStuff(ArgumentMatchers.any())
        testDriver.advanceWallClockTime(Duration.ofMinutes(30))
        kafkaEvent = journalforingOperationsKVStore["Test123"]
        assertNull(kafkaEvent)
        assertEquals("123456789", outputTopic.readValue().journalpostId)
    }
}
