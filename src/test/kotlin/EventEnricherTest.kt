import junit.framework.TestCase
import no.nav.helse.flex.Environment
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.operations.eventenricher.EventEnricher
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost.Bruker
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient
import no.nav.helse.flex.operations.eventenricher.saf.SafClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner
import java.util.*

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("no.nav.helse.flex.Environment")
@PrepareForTest(EventEnricher::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class EventEnricherTest {
    private lateinit var mockSafClient: SafClient
    private lateinit var mockPdlClient: PdlClient
    private lateinit var mockFkvClient: FkvClient
    private lateinit var eventEnricher: EventEnricher
    private lateinit var fkvKrutkoder: FkvKrutkoder

    @Before
    fun setup() {
        mockSafClient = PowerMockito.mock(SafClient::class.java)
        mockPdlClient = PowerMockito.mock(PdlClient::class.java)
        mockFkvClient = PowerMockito.mock(FkvClient::class.java)
        fkvKrutkoder = PowerMockito.mock(FkvKrutkoder::class.java)
        PowerMockito.whenNew(SafClient::class.java).withNoArguments().thenReturn(mockSafClient)
        PowerMockito.whenNew(PdlClient::class.java).withNoArguments().thenReturn(mockPdlClient)
        PowerMockito.whenNew(FkvClient::class.java).withNoArguments().thenReturn(mockFkvClient)
        PowerMockito.`when`(mockFkvClient.fetchKrutKoder()).thenReturn(fkvKrutkoder)
        PowerMockito.spy(Environment::class.java)
        PowerMockito.doReturn("automatiskSkjema.json")
            .`when`(Environment::class.java, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING")
        eventEnricher = EventEnricher()
    }

    private fun mockJournalpost(
        journalpostId: String,
        brevkode: String?,
        journalpostStatus: String,
        tema: String
    ): Journalpost {
        val mockBruker = Bruker("1234", "FNR")
        val mockJournalpost = Journalpost()
        mockJournalpost.setTittel("Test Journalpost")
        mockJournalpost.journalpostId = journalpostId
        mockJournalpost.journalforendeEnhet = "1111"
        mockJournalpost.dokumenter = listOf(Dokument(brevkode, "dokTittel", "123"))
        mockJournalpost.bruker = mockBruker
        mockJournalpost.journalstatus = journalpostStatus
        mockJournalpost.tema = tema
        return mockJournalpost
    }

    @Test
    fun test_data_on_event() {
        val journalpost = mockJournalpost("123456789", "NAV 08-07.04 D", "M", "SYK")
        PowerMockito.`when`(mockSafClient.retriveJournalpost(Mockito.anyString()))
            .thenReturn(journalpost)
        PowerMockito.`when`(
            mockPdlClient.retrieveIdenterFromPDL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
            )
        )
            .thenReturn(listOf(Ident("1234567891113", false, "AKTORID")))
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent)
        TestCase.assertEquals(journalpost, enrichedKafkaEvent.journalpost)
        TestCase.assertEquals("1234567891113", enrichedKafkaEvent.aktoerId)
        TestCase.assertFalse(enrichedKafkaEvent.isToManuell)
    }

    @Test(expected = InvalidJournalpostStatusException::class)
    fun test_brevkode_null() {
        val journalpost = mockJournalpost("123456789", null, "J", "SYK")
        PowerMockito.`when`(mockSafClient.retriveJournalpost(Mockito.anyString()))
            .thenReturn(journalpost)
        PowerMockito.`when`(
            mockPdlClient.retrieveIdenterFromPDL(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString()
            )
        )
            .thenReturn(listOf(Ident("1234567891113", false, "AKTORID")))
        val event = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO")
        val enrichedKafkaEvent = EnrichedKafkaEvent(event)
        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent)
    }
}
