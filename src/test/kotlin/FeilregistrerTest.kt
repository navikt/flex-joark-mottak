import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.flex.operations.Feilregistrer
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import util.TestUtils.mockEnrichedKafkaevent

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeilregistrerTest {
    private val mockOppgaveClient: OppgaveClient = mockk()

    private lateinit var feilregistrer: Feilregistrer

    @BeforeAll
    fun setup() {
        feilregistrer = Feilregistrer(mockOppgaveClient)
    }

    @Test
    fun test_ignore_oppgave_null() {
        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        assertNull(enrichedKafkaEvent.oppgave)

        feilregistrer.feilregistrerOppgave(enrichedKafkaEvent)
        verify(exactly = 0) { mockOppgaveClient.updateOppgave(any()) }
    }

    @Test
    fun test_attempt_feilreg() {
        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        val oppgave = Oppgave()
        enrichedKafkaEvent.oppgave = oppgave

        assertNotEquals("FEILREGISTRERT", oppgave.status)
        feilregistrer.feilregistrerOppgave(enrichedKafkaEvent)

        verify {
            mockOppgaveClient.updateOppgave(
                withArg {
                    assertEquals("FEILREGISTRERT", oppgave.status)
                }
            )
        }
    }
}
