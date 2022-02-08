import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.spyk
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.operations.eventenricher.EventEnricher
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient
import no.nav.helse.flex.operations.eventenricher.saf.SafClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder
import no.nav.helse.flex.operations.generell.felleskodeverk.TemaSkjemaData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockJournalpost
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventEnricherTest {
    val mockSafClient: SafClient = mockk()

    val mockPdlClient: PdlClient = mockk()

    val mockFkvClient: FkvClient = mockk()

    val fkvKrutkoder: FkvKrutkoder = spyk()

    lateinit var eventEnricher: EventEnricher

    @BeforeAll
    fun setup() {
        every { mockFkvClient.fetchKrutKoder() } returns fkvKrutkoder

        eventEnricher = EventEnricher(
            safClient = mockSafClient,
            pdlClient = mockPdlClient,
            fkvClient = mockFkvClient
        )
    }

    @Test
    fun test_data_on_event() {
        val journalpost = mockJournalpost("123456789", "NAV 08-07.04D", "SYK", "M")
        every { mockSafClient.retriveJournalpost(any()) } returns journalpost
        every { mockPdlClient.retrieveIdenterFromPDL(any(), any(), any()) } returns listOf(
            Ident(
                "AKTORID",
                "1234567891113"
            )
        )
        every { fkvKrutkoder.getTemaSkjema(any()) } returns TemaSkjemaData(
            "test", "NAV 08-07.04D", "ab0434", ""
        )

        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)

        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent)
        assertEquals(journalpost, enrichedKafkaEvent.journalpost)
        assertEquals("1234567891113", enrichedKafkaEvent.aktoerId)
        assertFalse(enrichedKafkaEvent.isToManuell)
    }

    @Test
    fun test_brevkode_null() {
        val journalpost = mockJournalpost("123456789", null, "SYK", "M")
        every { mockSafClient.retriveJournalpost(any()) } returns journalpost
        every { mockPdlClient.retrieveIdenterFromPDL(any(), any(), any()) } returns listOf(
            Ident(
                "AKTORID",
                "1234567891113"
            )
        )

        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent)
    }
}
