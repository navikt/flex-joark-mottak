import no.nav.helse.flex.Environment
import no.nav.helse.flex.operations.generell.GenerellOperations
import no.nav.helse.flex.operations.generell.oppgave.CreateOppgaveData
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.powermock.api.mockito.PowerMockito.doReturn
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.internal.WhiteboxImpl
import util.TestUtils.mockEnrichedKafkaevent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@RunWith(PowerMockRunner::class)
@SuppressStaticInitializationFor("no.nav.helse.flex.Environment")
@PrepareForTest(GenerellOperations::class, CreateOppgaveData::class)
@PowerMockIgnore("com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*")
class GenerellOperationsTest {
    private val mockOppgaveClient = mock(OppgaveClient::class.java)
    private lateinit var generellOperations: GenerellOperations

    @Before
    fun setup() {
        whenNew(OppgaveClient::class.java).withNoArguments().thenReturn(mockOppgaveClient)
        spy(Environment::class.java)
        doReturn("automatiskSkjema.json")
            .`when`(Environment::class.java, "getEnvVar", "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING")
        generellOperations = GenerellOperations()
    }

    @Test
    fun test_oppgave_saved_on_event() {
        val enrichedKafkaEvent = mockEnrichedKafkaevent()
        val createOppgave = Oppgave()
        createOppgave.id = "123"
        `when`(mockOppgaveClient!!.createOppgave(ArgumentMatchers.any())).thenReturn(createOppgave)
        WhiteboxImpl.invokeMethod<Any>(generellOperations, "createOppgave", enrichedKafkaEvent)
        assertEquals(createOppgave, enrichedKafkaEvent.oppgave)
        assertEquals("123", enrichedKafkaEvent.oppgaveId)
    }

    @Test
    fun test_oppgave_frist() {
        val morning = LocalDateTime.parse("2021-10-18T10:49:35")
        val evening = LocalDateTime.parse("2021-10-18T14:49:35")
        val fridayEvening = LocalDateTime.parse("2021-10-22T10:49:35")
        val holiday = LocalDateTime.parse("2021-12-24T15:49:35")
        val cutoff = LocalTime.of(12, 0, 0)
        mockStatic(LocalTime::class.java)
        `when`(
            LocalTime.of(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt()
            )
        ).thenReturn(cutoff)
        `when`(LocalTime.now()).thenReturn(
            morning.toLocalTime(),
            evening.toLocalTime(),
            fridayEvening.toLocalTime(),
            holiday.toLocalTime()
        )
        whenNew(Date::class.java).withNoArguments().thenReturn(
            Date.from(morning.atZone(ZoneId.systemDefault()).toInstant()),
            Date.from(evening.atZone(ZoneId.systemDefault()).toInstant()),
            Date.from(fridayEvening.atZone(ZoneId.systemDefault()).toInstant()),
            Date.from(holiday.atZone(ZoneId.systemDefault()).toInstant())
        )

        val cod1 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("Journalpost mottatt på morgenen", "2021-10-21", cod1.fristFerdigstillelse)

        val cod2 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("Journalpost mottatt på kvelden", "2021-10-22", cod2.fristFerdigstillelse)

        val cod3 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("Journalpost mottatt fredag", "2021-10-27", cod3.fristFerdigstillelse)

        val cod4 = CreateOppgaveData("123", "123", "SYK", "", "", "", 3)
        assertEquals("Journalpost mottatt før helligdag", "2021-12-30", cod4.fristFerdigstillelse)
    }
}
