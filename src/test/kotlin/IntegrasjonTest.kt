import com.fasterxml.jackson.module.kotlin.readValue
import mock.*
import no.nav.helse.flex.journalpost.FerdigstillJournalpostRequest
import no.nav.helse.flex.objectMapper
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class IntegrasjonTest : BaseTestClass() {
    @Test
    fun `Mottar journalpost som allerede er ferdig arkivert i sykepengesoknad-arkivering-oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                DigitalSoknadPerson.kafkaEvent
            )
        ).get()

        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS) shouldBeEqualTo null
    }

    @Test
    fun `Papir sykepengesøknad som skal journalføres`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                PapirSoknadPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOppdaterJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!

        requestHarOppgave.requestLine shouldBeEqualTo "GET /api/v1/oppgaver?statuskategori=AAPEN&oppgavetype=JFR&oppgavetype=FDR&journalpostId=${PapirSoknadPerson.journalpostId} HTTP/1.1"

        requestOpprettOppgave.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo PapirSoknadPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo "ab0434"
        body.behandlingstype shouldBeEqualTo null

        requestOppdaterJournalpost.method shouldBeEqualTo "PUT"
        requestOppdaterJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.journalpostId}"

        requestFerdigstillJournalpost.method shouldBeEqualTo "PATCH"
        requestFerdigstillJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.journalpostId}/ferdigstill"
        objectMapper.readValue<FerdigstillJournalpostRequest>(requestFerdigstillJournalpost.body.readUtf8()).journalfoerendeEnhet shouldBeEqualTo "9999"
    }

    @Test
    fun `Inntektsopplysninger journalpost som skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                InntektsopplysningerPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo InntektsopplysningerPerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo InntektsopplysningerPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0004"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Klage journalpost som skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                KlagePerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo KlagePerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo KlagePerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Utlansk søknad om sykepenger som skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                UtlanskPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo UtlanskPerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo UtlanskPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0106"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost uten brevkode skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                BrevløsPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo BrevløsPerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo BrevløsPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost med ukjent brevkode skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                UkjentBrevkodePerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo UkjentBrevkodePerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo UkjentBrevkodePerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost uten person skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                JournalpostUtenPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo JournalpostUtenPerson.journalpostId

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo JournalpostUtenPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0106"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Mottar journalpost som vi skal ingnorere`() {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                InntektsmeldingPerson.kafkaEvent
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestHarOppgave shouldBeEqualTo null

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @AfterAll
    fun `Kodeverket ble bare kalt en gang og resultatet ble chacet`() {
        val request = kodeverkMockWebServer.takeRequest(1, TimeUnit.SECONDS)!!
        request.requestUrl?.encodedPath shouldBeEqualTo "/api/v1/hierarki/TemaSkjemaGjelder/noder"

        kodeverkMockWebServer.requestCount shouldBeEqualTo 1
    }
}
