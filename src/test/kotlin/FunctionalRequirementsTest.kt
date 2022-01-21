import no.nav.helse.flex.Environment
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient
import no.nav.helse.flex.operations.generell.GenerellOperations
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito.doReturn
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.internal.WhiteboxImpl
import util.TestUtils.mockEnrichedKafkaevent
import util.TestUtils.mockJournalpost

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("no.nav.helse.flex.Environment")
@PrepareForTest(GenerellOperations::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class FunctionalRequirementsTest {
    private val mockOppgaveClient: OppgaveClient = mock(OppgaveClient::class.java)
    private val mockFkvClient: FkvClient = mock(FkvClient::class.java)
    private val mockFkvKrutkoder: FkvKrutkoder = mock(FkvKrutkoder::class.java)
    private val mockPdlClient: PdlClient = mock(PdlClient::class.java)
    private lateinit var generellOperations: GenerellOperations

    @Before
    fun setup() {
        whenNew(OppgaveClient::class.java).withNoArguments().thenReturn(mockOppgaveClient)
        whenNew(FkvClient::class.java).withNoArguments().thenReturn(mockFkvClient)
        whenNew(PdlClient::class.java).withNoArguments().thenReturn(mockPdlClient)
        `when`(mockFkvClient.fetchKrutKoder()).thenReturn(mockFkvKrutkoder)
        spy(Environment::class.java)
        doReturn("automatiskSkjema.json").`when`(
            Environment::class.java, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING"
        )

        generellOperations = GenerellOperations()
    }

    @Test
    fun test_req_all_dok_tilter_set() {
        val jp = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        val vedlegg = listOf(
            Dokument("dontCare", "tittelVedlegg", "321"),
            Dokument("dontCare", "tittelVedlegg", "123")
        )
        val dokumentList: MutableList<Dokument> = ArrayList()
        dokumentList.addAll(jp.dokumenter)
        dokumentList.addAll(vedlegg)
        jp.dokumenter = dokumentList

        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        enrichedKafkaEvent.journalpost = jp
        val dokTitlerSet = WhiteboxImpl.invokeMethod<Boolean>(
            generellOperations,
            "hasValidDokumentTitler",
            enrichedKafkaEvent
        )

        assertEquals(true, dokTitlerSet)
    }

    @Test
    fun test_req_dok_tilter_not_set() {
        val jp = mockJournalpost("123456789", "NAV 21-04.05", "SYK", "M")
        val vedlegg = listOf(
            Dokument("dontCare", "tittelVedlegg", "321"),
            Dokument("dontCare", null, "123")
        )
        val dokumentList: MutableList<Dokument> = ArrayList()
        dokumentList.addAll(jp.dokumenter)
        dokumentList.addAll(vedlegg)
        jp.dokumenter = dokumentList

        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        enrichedKafkaEvent.journalpost = jp
        val dokTitlerSet = WhiteboxImpl.invokeMethod<Boolean>(
            generellOperations,
            "hasValidDokumentTitler",
            enrichedKafkaEvent
        )
        assertEquals(false, dokTitlerSet)
    }
}
