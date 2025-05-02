@file:Suppress("ktlint:standard:max-line-length")

import com.fasterxml.jackson.module.kotlin.readValue
import mock.*
import no.nav.helse.flex.journalpost.FerdigstillJournalpostRequest
import no.nav.helse.flex.objectMapper
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class IntegrasjonTest : FellesTestOppsett() {
    @Test
    fun `Journalpost som allerede er ferdig arkivert i sykepengesoknad-arkivering-oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    DigitalSoknadPerson.kafkaEvent,
                ),
            ).get()

        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS) shouldBeEqualTo null
    }

    @Test
    fun `Papirsykepengesøknad skal journalføres`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    PapirSoknadPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOppdaterJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!

        requestHarOppgave.requestLine shouldBeEqualTo
            "GET /api/v1/oppgaver?statuskategori=AAPEN&oppgavetype=JFR&oppgavetype=FDR&journalpostId=${PapirSoknadPerson.JOURNALPOST_ID} HTTP/1.1"

        requestOpprettOppgave.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo PapirSoknadPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo "ab0434"
        body.behandlingstype shouldBeEqualTo null

        requestOppdaterJournalpost.method shouldBeEqualTo "PUT"
        requestOppdaterJournalpost.requestUrl?.encodedPath shouldBeEqualTo
            "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}"

        requestFerdigstillJournalpost.method shouldBeEqualTo "PATCH"
        requestFerdigstillJournalpost.requestUrl?.encodedPath shouldBeEqualTo
            "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}/ferdigstill"
        objectMapper
            .readValue<FerdigstillJournalpostRequest>(
                requestFerdigstillJournalpost.body.readUtf8(),
            ).journalfoerendeEnhet shouldBeEqualTo
            "9999"
    }

    @Test
    fun `Inntektsopplysninger journalpost skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    InntektsopplysningerPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo InntektsopplysningerPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo InntektsopplysningerPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0004"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost med klage skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    KlagePerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo KlagePerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo KlagePerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Utenlandsk søknad om sykepenger skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    UtenlandskPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo UtenlandskPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo UtenlandskPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0106"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost uten brevkode skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    BrevløsPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo BrevløsPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo BrevløsPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost med ukjent brevkode skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    UkjentBrevkodePerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo UkjentBrevkodePerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo UkjentBrevkodePerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo null

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost uten person skal ha JFR oppgave`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    JournalpostUtenPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)

        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo JournalpostUtenPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBeEqualTo "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo JournalpostUtenPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo null
        body.behandlingstype shouldBeEqualTo "ae0106"

        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Journalpost som skal ignoreres`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    InntektsmeldingPerson.kafkaEvent,
                ),
            ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestHarOppgave shouldBeEqualTo null

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Det opprettes JFR oppgave for papirsykepengesoknad med organisasjonsnummer som identifikator`() {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    PapirSoknadMedOrgNrPerson.kafkaEvent,
                ),
            ).get()

        // Har oppgave kalles først når oppgave oppretted, og deretter når det opprettes en manuell oppgave.
        repeat(2) {
            val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
            requestHarOppgave.method shouldBeEqualTo "GET"
            requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
            requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo PapirSoknadMedOrgNrPerson.JOURNALPOST_ID
        }

        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestOpprettOppgave.method shouldBeEqualTo "POST"

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBe null

        val oppgaveRequestBody = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        oppgaveRequestBody.journalpostId shouldBeEqualTo PapirSoknadMedOrgNrPerson.JOURNALPOST_ID
        oppgaveRequestBody.orgnr shouldBeEqualTo PapirSoknadMedOrgNrPerson.ORGNR
        oppgaveRequestBody.aktoerId shouldBe null
        oppgaveRequestBody.tema shouldBeEqualTo "SYK"
        oppgaveRequestBody.behandlingstema shouldBeEqualTo "ab0434"
        oppgaveRequestBody.behandlingstype shouldBe null
    }

    @AfterAll
    fun `Kodeverket ble bare kalt en gang og resultatet ble chacet`() {
        val request = kodeverkMockWebServer.takeRequest(1, TimeUnit.SECONDS)!!
        request.requestUrl?.encodedPath shouldBeEqualTo "/api/v1/hierarki/TemaSkjemaGjelder/noder"

        kodeverkMockWebServer.requestCount shouldBeEqualTo 1
    }
}
