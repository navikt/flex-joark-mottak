import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import no.nav.helse.flex.Environment
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException
import no.nav.helse.flex.operations.SkjemaMetadata
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.generell.GenerellOperations
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockEnrichedKafkaevent
import util.TestUtils.mockJournalpost

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FunctionalRequirementsTest {
    private val mockOppgaveClient: OppgaveClient = mockk(relaxed = true)
    private val mockSkjemaMetadata: SkjemaMetadata = mockk()
    private lateinit var generellOperations: GenerellOperations

    @BeforeAll
    fun setup() {
        mockkObject(Environment)
        every { Environment.getEnvVar("STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING") } returns "automatiskSkjema.json"

        generellOperations = GenerellOperations(
            mockOppgaveClient,
            mockSkjemaMetadata
        )
    }

    @Test
    fun test_req_all_dok_tilter_set() {
        val jp = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        jp.dokumenter += listOf(
            Dokument("cafe", "tittelVedlegg", "321"),
            Dokument("taxi", "tittelVedlegg", "123")
        )

        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        enrichedKafkaEvent.journalpost = jp

        every { mockSkjemaMetadata.getOppgavetype(any(), any()) } returns "SOK"
        every { mockSkjemaMetadata.getFrist(any(), any()) } returns 3

        generellOperations.executeProcess(enrichedKafkaEvent)

        verify { mockOppgaveClient.createOppgave(any()) }
    }

    @Test
    fun test_req_dok_tilter_not_set() {
        val jp = mockJournalpost("123456789", "NAV 06-04.04", "SYK", "M")
        jp.dokumenter += listOf(
            Dokument("cafe", "tittelVedlegg", "321"),
            Dokument("taxi", null, "123")
        )

        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        enrichedKafkaEvent.journalpost = jp

        assertThrows<FunctionalRequirementException> {
            generellOperations.executeProcess(enrichedKafkaEvent)
        }
    }
}
