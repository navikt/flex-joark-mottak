package operations;

import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.exceptions.InvalidJournalpostStatusException;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.jfr.generell.infrastructure.kafka.KafkaEvent;
import no.nav.jfr.generell.operations.generell.felleskodeverk.*;
import no.nav.jfr.generell.operations.eventenricher.pdl.Ident;
import no.nav.jfr.generell.operations.eventenricher.EventEnricher;
import no.nav.jfr.generell.operations.eventenricher.pdl.PdlClient;
import no.nav.jfr.generell.operations.eventenricher.saf.SafClient;
import no.nav.jfr.generell.operations.eventenricher.journalpost.Dokument;
import no.nav.jfr.generell.operations.eventenricher.journalpost.Journalpost;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"no.nav.jfr.generell.Environment"})
@PrepareForTest({EventEnricher.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class EventEnricherTest {

    private SafClient mockSafClient;
    private PdlClient mockPdlClient;
    private FkvClient mockFkvClient;
    private EventEnricher eventEnricher;
    private FkvKrutkoder fkvKrutkoder;

    @Before
    public void setup() throws Exception {
        mockSafClient = mock(SafClient.class);
        mockPdlClient = mock(PdlClient.class);
        mockFkvClient = mock(FkvClient.class);
        fkvKrutkoder = mock(FkvKrutkoder.class);
        PowerMockito.whenNew(SafClient.class).withNoArguments().thenReturn(mockSafClient);
        PowerMockito.whenNew(PdlClient.class).withNoArguments().thenReturn(mockPdlClient);
        PowerMockito.whenNew(FkvClient.class).withNoArguments().thenReturn(mockFkvClient);
        PowerMockito.when(mockFkvClient.fetchKrutKoder()).thenReturn(fkvKrutkoder);
        spy(Environment.class);
        doReturn("automatiskSkjema.json").when(Environment.class, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING");
        eventEnricher = new EventEnricher();
    }

    private Journalpost mockJournalpost(String journalpostId, String brevkode, String journalpostStatus, String tema){
        Journalpost.Bruker mockBruker;

        mockBruker = new Journalpost.Bruker("1234", "FNR");

        Journalpost mockJournalpost = new Journalpost();
        mockJournalpost.setTittel("Test Journalpost");
        mockJournalpost.setJournalpostId(journalpostId);
        mockJournalpost.setJournalforendeEnhet("1111");
        mockJournalpost.setDokumenter(Collections.singletonList(new Dokument(brevkode, "dokTittel", "123")));
        mockJournalpost.setBruker(mockBruker);
        mockJournalpost.setJournalstatus(journalpostStatus);
        mockJournalpost.setTema(tema);

        return mockJournalpost;
    }

    @Test
    public void test_data_on_event() throws Exception {
        final Journalpost journalpost = mockJournalpost("123456789", "NAV 06-04.04", "M", "GEN");
        PowerMockito.when(mockSafClient.retriveJournalpost(Mockito.anyString()))
                .thenReturn(journalpost);
        PowerMockito.when(mockPdlClient.retrieveIdenterFromPDL(Mockito.anyString(),Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Collections.singletonList(new Ident("1234567891113", false, "AKTORID")));
        final KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GEN", "NAV_NO");
        final EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent);

        assertEquals(journalpost, enrichedKafkaEvent.getJournalpost());
        assertEquals("1234567891113", enrichedKafkaEvent.getAktoerId());
        assertFalse(enrichedKafkaEvent.isToManuell());
    }

    @Test (expected = InvalidJournalpostStatusException.class)
    public void test_brevkode_null() throws Exception {
        final Journalpost journalpost = mockJournalpost("123456789", null, "J", "GRU");
        PowerMockito.when(mockSafClient.retriveJournalpost(Mockito.anyString()))
                .thenReturn(journalpost);
        PowerMockito.when(mockPdlClient.retrieveIdenterFromPDL(Mockito.anyString(),Mockito.anyString(), Mockito.anyString()))
                .thenReturn(Collections.singletonList(new Ident("1234567891113", false, "AKTORID")));
        final KafkaEvent event = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "GRU", "NAV_NO");
        final EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(event);
        eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent);
    }
}
