import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
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

    lateinit var generellOperations: GenerellOperations

    @BeforeAll
    fun setup() {
        generellOperations = GenerellOperations(mockOppgaveClient)
    }

    @Test
    fun test_oppgave_saved_on_event() {
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
        val fridayMorning = LocalDateTime.parse("2021-10-22T10:49:35")
        val fridayEvening = LocalDateTime.parse("2021-10-22T14:49:35")
        val saturday = LocalDateTime.parse("2021-10-23T10:49:35")
        val sunday = LocalDateTime.parse("2021-10-24T10:49:35")

        mockkStatic(LocalDateTime::class)
        every { LocalDateTime.now() } returnsMany listOf(
            morning,
            evening,
            fridayMorning,
            fridayEvening,
            saturday,
            sunday
        )

        val cod1 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-21", cod1.fristFerdigstillelse, "Journalpost mottatt på morgenen")

        val cod2 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-22", cod2.fristFerdigstillelse, "Journalpost mottatt på kvelden")

        val cod3 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-27", cod3.fristFerdigstillelse, "Journalpost mottatt fredag før kl 12")

        val cod4 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-28", cod4.fristFerdigstillelse, "Journalpost mottatt fredag etter kl 12")

        val cod5 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-27", cod5.fristFerdigstillelse, "Journalpost mottatt lørdag")

        val cod6 = CreateOppgaveData(aktoerId = "123", journalpostId = "123", tema = "SYK", behandlingstema = "", behandlingstype = "", oppgavetype = "", frist = 3)
        assertEquals("2021-10-27", cod6.fristFerdigstillelse, "Journalpost mottatt søndag")
    }
}
