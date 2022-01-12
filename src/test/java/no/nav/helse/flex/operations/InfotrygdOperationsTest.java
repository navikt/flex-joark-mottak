package no.nav.helse.flex.operations;

import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient;
import no.nav.helse.flex.operations.generell.GenerellOperations;
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient;
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder;
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData;
import no.nav.helse.flex.operations.generell.oppgave.Oppgave;
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.internal.WhiteboxImpl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.helse.flex.Environment"})
@PrepareForTest({GenerellOperations.class, CreateOppgaveData.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class InfotrygdOperationsTest {

//    private InfotrygdFacadeClient mockInfotrygdFacadeClient;
    private GenerellOperations generellOperations;
    private OppgaveClient mockOppgaveClient;
    private FkvClient mockFkvClient;
    private FkvKrutkoder mockFkvKrutkoder;
    private PdlClient mockPdlClient;

    @Before
    public void setup() throws Exception {
        mockOppgaveClient = mock(OppgaveClient.class);
        PowerMockito.whenNew(OppgaveClient.class).withNoArguments().thenReturn(mockOppgaveClient);
        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING");

        generellOperations = new GenerellOperations();
    }

    @Test
    public void test_oppgave_saved_on_event() throws Exception {
        EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        Oppgave createOppgave = new Oppgave();
        createOppgave.setId("123");

        when(mockOppgaveClient.createOppgave(any())).thenReturn(createOppgave);

        WhiteboxImpl.invokeMethod(generellOperations, "createOppgave", enrichedKafkaEvent);
        Assert.assertEquals(createOppgave, enrichedKafkaEvent.getOppgave());
        Assert.assertEquals("123", enrichedKafkaEvent.getOppgaveId());
    }

    @Test
    public void test_oppgave_frist() throws Exception {
        LocalDateTime morning =  LocalDateTime.parse("2021-10-18T10:49:35");
        LocalDateTime evening =  LocalDateTime.parse("2021-10-18T14:49:35");
        LocalDateTime fridayEvening =  LocalDateTime.parse("2021-10-22T10:49:35");
        LocalDateTime holiday = LocalDateTime.parse("2021-12-24T15:49:35");
        LocalTime cutoff = LocalTime.of(12,0,0);
        PowerMockito.mockStatic(LocalTime.class);
        PowerMockito.when(LocalTime.of(anyInt(), anyInt(), anyInt())).thenReturn(cutoff);
        PowerMockito.when(LocalTime.now()).thenReturn(morning.toLocalTime(), evening.toLocalTime(), fridayEvening.toLocalTime(), holiday.toLocalTime());

        PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(
                Date.from(morning.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(evening.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(fridayEvening.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(holiday.atZone(ZoneId.systemDefault()).toInstant()));

        CreateOppgaveData cod1 = new CreateOppgaveData("123", "123", "KON", "", "", "",3 );
        Assert.assertEquals("Journalpost mottatt på morgenen","2021-10-21", cod1.getFristFerdigstillelse());
        CreateOppgaveData cod2 = new CreateOppgaveData("123", "123", "KON", "", "", "", 3);
        Assert.assertEquals("Journalpost mottatt på kvelden", "2021-10-22", cod2.getFristFerdigstillelse());
        CreateOppgaveData cod3 = new CreateOppgaveData("123", "123", "KON", "", "", "", 3);
        Assert.assertEquals("Journalpost mottatt fredag", "2021-10-27", cod3.getFristFerdigstillelse());
        CreateOppgaveData cod4 = new CreateOppgaveData("123", "123", "KON", "", "", "" , 3);
        Assert.assertEquals("Journalpost mottatt før helligdag", "2021-12-30", cod4.getFristFerdigstillelse());
    }
}
