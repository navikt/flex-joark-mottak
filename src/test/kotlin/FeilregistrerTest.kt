import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.operations.Feilregistrer
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import util.TestUtils.mockEnrichedKafkaevent

@RunWith(PowerMockRunner::class)
@PrepareForTest(Feilregistrer::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class FeilregistrerTest {
    private val mockOppgaveClient = mock(OppgaveClient::class.java)
    private lateinit var feilregistrer: Feilregistrer

    @Before
    fun setup() {
        whenNew(OppgaveClient::class.java).withNoArguments().thenReturn(mockOppgaveClient)
        feilregistrer = Feilregistrer()
    }

    @Test
    fun test_ignore_oppgave_null() {
        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        Assert.assertNull(enrichedKafkaEvent.oppgave)
        feilregistrer.feilregistrerOppgave("id", enrichedKafkaEvent)
        PowerMockito.verifyZeroInteractions(mockOppgaveClient)
    }

    @Test
    fun test_attempt_feilreg() {
        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        val oppgave = Oppgave()
        enrichedKafkaEvent.oppgave = oppgave
        Assert.assertNotEquals("FEILREGISTRERT", oppgave.status)
        val argument = ArgumentCaptor.forClass(
            EnrichedKafkaEvent::class.java
        )
        feilregistrer.feilregistrerOppgave("id", enrichedKafkaEvent)
        Mockito.verify(mockOppgaveClient).updateOppgave(argument.capture())
        val ferdigstillOppgave = argument.value.oppgave
        Assert.assertEquals("FEILREGISTRERT", ferdigstillOppgave.status)
    }
}
