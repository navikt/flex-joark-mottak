package no.nav.helse.flex.operations;

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.operations.generell.oppgave.Oppgave;
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Feilregistrer.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
@Ignore
public class FeilregistrerTest {

    private OppgaveClient mockOppgaveClient;
    private Feilregistrer feilregistrer;

    @Before
    public void setup() throws Exception {
        mockOppgaveClient = mock(OppgaveClient.class);
        PowerMockito.whenNew(OppgaveClient.class).withNoArguments().thenReturn(mockOppgaveClient);
        this.feilregistrer = new Feilregistrer();
    }

    @Test
    public void test_ignore_oppgave_null(){
        EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        Assert.assertNull(enrichedKafkaEvent.getOppgave());
        feilregistrer.feilregistrerOppgave("id", enrichedKafkaEvent);
        PowerMockito.verifyZeroInteractions(mockOppgaveClient);
    }

    @Test
    public void test_attempt_feilreg() throws Exception {
        EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        Oppgave oppgave = new Oppgave();
        enrichedKafkaEvent.setOppgave(oppgave);
        Assert.assertNotEquals("FEILREGISTRERT", oppgave.getStatus());

        ArgumentCaptor<EnrichedKafkaEvent> argument = ArgumentCaptor.forClass(EnrichedKafkaEvent.class);
        feilregistrer.feilregistrerOppgave("id", enrichedKafkaEvent);
        Mockito.verify(mockOppgaveClient).updateOppgave(argument.capture());
        Oppgave ferdigstillOppgave = argument.getValue().getOppgave();

        Assert.assertEquals("FEILREGISTRERT", ferdigstillOppgave.getStatus());
    }
}
