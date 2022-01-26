import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import no.nav.helse.flex.Environment
import no.nav.helse.flex.operations.SkjemaMetadata
import no.nav.helse.flex.operations.generell.GenerellOperations
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockEnrichedKafkaevent
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenerellOperationsTest {
    val mockOppgaveClient: OppgaveClient = mockk(relaxed = true)
    val mockSkjemaMetadata: SkjemaMetadata = mockk()

    lateinit var generellOperations: GenerellOperations

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
    fun test_oppgave_saved_on_event() {
        every { mockSkjemaMetadata.getOppgavetype(any(), any()) } returns "SOK"
        every { mockSkjemaMetadata.getFrist(any(), any()) } returns 3

        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        val createOppgave = Oppgave()
        createOppgave.id = "123"

        every { mockOppgaveClient.createOppgave(any()) } returns createOppgave

        generellOperations.executeProcess(enrichedKafkaEvent)

        assertEquals(createOppgave, enrichedKafkaEvent.oppgave)
        assertEquals("123", enrichedKafkaEvent.getOppgaveId())
    }

    @Test
    fun test_oppgave_frist() {
        val morning = LocalDateTime.parse("2021-10-18T10:49:35")
        val evening = LocalDateTime.parse("2021-10-18T14:49:35")
        val fridayEvening = LocalDateTime.parse("2021-10-22T10:49:35")
        val holiday = LocalDateTime.parse("2021-12-24T15:49:35")

        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returnsMany listOf(
            morning,
            evening,
            fridayEvening,
            holiday
        )

        val cod1 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("2021-10-21", cod1.fristFerdigstillelse, "Journalpost mottatt på morgenen")

        val cod2 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("2021-10-22", cod2.fristFerdigstillelse, "Journalpost mottatt på kvelden")

        val cod3 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("2021-10-27", cod3.fristFerdigstillelse, "Journalpost mottatt fredag")

        val cod4 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("2021-12-30", cod4.fristFerdigstillelse, "Journalpost mottatt før helligdag")
    }
}
