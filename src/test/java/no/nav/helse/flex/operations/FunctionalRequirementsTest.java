package no.nav.helse.flex.operations;

import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.eventenricher.pdl.PdlClient;
import no.nav.helse.flex.operations.generell.GenerellOperations;
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvClient;
import no.nav.helse.flex.operations.generell.felleskodeverk.FkvKrutkoder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.helse.flex.Environment"})
@PrepareForTest({GenerellOperations.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class FunctionalRequirementsTest {

    private GenerellOperations generellOperations;
    private OppgaveClient mockOppgaveClient;
    private FkvClient mockFkvClient;
    private FkvKrutkoder mockFkvKrutkoder;
    private PdlClient mockPdlClient;

    @Before
    public void setup() throws Exception {
        mockOppgaveClient = mock(OppgaveClient.class);
        mockFkvClient = mock(FkvClient.class);
        mockFkvKrutkoder = mock(FkvKrutkoder.class);
        mockPdlClient = mock(PdlClient.class);
        PowerMockito.whenNew(OppgaveClient.class).withNoArguments().thenReturn(mockOppgaveClient);
        PowerMockito.whenNew(FkvClient.class).withNoArguments().thenReturn(mockFkvClient);
        PowerMockito.whenNew(PdlClient.class).withNoArguments().thenReturn(mockPdlClient);
        PowerMockito.when(mockFkvClient.fetchKrutKoder()).thenReturn(mockFkvKrutkoder);
        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING");

        generellOperations = new GenerellOperations();
    }

    @Test
    public void test_req_all_dok_tilter_set() throws Exception{
        Journalpost jp = TestUtils.mockJournalpost("123456789", "NAV 06-04.04", "GRU", "M");
        List<Dokument> vedlegg = Arrays.asList(
                new Dokument("dontCare", "tittelVedlegg", "321"),
                new Dokument("dontCare", "tittelVedlegg", "123"));
        List<Dokument> dokumentList = new ArrayList<>();
        dokumentList.addAll(jp.getDokumenter());
        dokumentList.addAll(vedlegg);
        jp.setDokumenter(dokumentList);
        final EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        enrichedKafkaEvent.setJournalpost(jp);
        Boolean dokTitlerSet = WhiteboxImpl.invokeMethod(generellOperations, "hasValidDokumentTitler", enrichedKafkaEvent);
        Assert.assertEquals(true, dokTitlerSet);
    }

    @Test
    public void test_req_dok_tilter_not_set() throws Exception{
        Journalpost jp = TestUtils.mockJournalpost("123456789", "NAV 21-04.05", "KON", "M");
        List<Dokument> vedlegg = Arrays.asList(
                new Dokument("dontCare", "tittelVedlegg", "321"),
                new Dokument("dontCare", null, "123"));
        List<Dokument> dokumentList = new ArrayList<>();
        dokumentList.addAll(jp.getDokumenter());
        dokumentList.addAll(vedlegg);
        jp.setDokumenter(dokumentList);
        final EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        enrichedKafkaEvent.setJournalpost(jp);
        final Boolean dokTitlerSet = WhiteboxImpl.invokeMethod(generellOperations, "hasValidDokumentTitler", enrichedKafkaEvent);
        Assert.assertEquals(false, dokTitlerSet);
    }
}
