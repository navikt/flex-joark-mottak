package no.nav.helse.flex.operations;

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.journalforing.JournalforingOperations;
import no.nav.helse.flex.operations.journalforing.dokarkiv.JournalpostAPIClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JournalforingOperations.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})

public class JournalforingOperationsTest {
    private JournalpostAPIClient mockJournalpostAPIClient;
    private JournalforingOperations journalforingOperations;

    @Before
    public void setup() throws Exception{
        mockJournalpostAPIClient = mock(JournalpostAPIClient.class);
        PowerMockito.whenNew(JournalpostAPIClient.class).withNoArguments().thenReturn(mockJournalpostAPIClient);
        journalforingOperations = new JournalforingOperations();
    }

    @Test
    public void test_edit_complete_journal() throws Exception {
        EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        Journalpost journalpost = TestUtils.mockJournalpost("123213213214", "NAV 06-04.04", "GRU", "MO");

        //PowerMockito.when(mockJournalpostClient.updateJournalpost(journalpost)).thenReturn(true, false);
        //PowerMockito.when(mockJournalpostClient.finalizeJournalpost(journalpost.getJournalpostId())).thenReturn(true,false);
        journalforingOperations.doAutomaticStuff(enrichedKafkaEvent);
    }

    @Test(expected = Exception.class)
    public void test_journalfoering_oppdater_fail() throws Exception {
        EnrichedKafkaEvent enrichedKafkaEvent = TestUtils.mockEnrichedKafkaevent();
        Journalpost journalpost = TestUtils.mockJournalpost("123213213214", "NAV 06-04.04", "GRU", "MO");
        PowerMockito.doThrow(new Exception()).when(mockJournalpostAPIClient).updateJournalpost(journalpost);
        journalforingOperations.doAutomaticStuff(enrichedKafkaEvent);
    }

    //@Test
    //Mangler ferdigstill feiler test. ToDo
    public void test_journalfoering_ferdigstill_feiler(){

    }
}
