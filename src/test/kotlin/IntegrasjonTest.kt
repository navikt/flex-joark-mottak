@file:Suppress("ktlint:standard:max-line-length")

import io.kotest.matchers.shouldBe
import mock.*
import no.nav.helse.flex.journalpost.FerdigstillJournalpostRequest
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
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

        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS) shouldBe null
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

        requestHarOppgave.requestLine shouldBe
            "GET /api/v1/oppgaver?statuskategori=AAPEN&oppgavetype=JFR&oppgavetype=FDR&journalpostId=${PapirSoknadPerson.JOURNALPOST_ID} HTTP/1.1"

        requestOpprettOppgave.requestLine shouldBe "POST /api/v1/oppgaver HTTP/1.1"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe PapirSoknadPerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe "ab0434"
        body.behandlingstype shouldBe null

        requestOppdaterJournalpost.method shouldBe "PUT"
        requestOppdaterJournalpost.url.encodedPath shouldBe
            "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}"

        requestFerdigstillJournalpost.method shouldBe "PATCH"
        requestFerdigstillJournalpost.url.encodedPath shouldBe
            "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}/ferdigstill"
        objectMapper
            .readValue<FerdigstillJournalpostRequest>(
                requestFerdigstillJournalpost.body!!.utf8(),
            ).journalfoerendeEnhet shouldBe
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe InntektsopplysningerPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe InntektsopplysningerPerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe "ae0004"

        requestFerdigstillJournalpost shouldBe null
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe KlagePerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe KlagePerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe null

        requestFerdigstillJournalpost shouldBe null
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe UtenlandskPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe UtenlandskPerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe "ae0106"

        requestFerdigstillJournalpost shouldBe null
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe BrevløsPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe BrevløsPerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe null

        requestFerdigstillJournalpost shouldBe null
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe UkjentBrevkodePerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe UkjentBrevkodePerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe null

        requestFerdigstillJournalpost shouldBe null
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

        requestHarOppgave.method shouldBe "GET"
        requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
        requestHarOppgave.url.queryParameter("journalpostId") shouldBe JournalpostUtenPerson.JOURNALPOST_ID

        requestOpprettOppgave.method shouldBe "POST"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBe JournalpostUtenPerson.JOURNALPOST_ID
        body.tema shouldBe "SYK"
        body.behandlingstema shouldBe null
        body.behandlingstype shouldBe "ae0106"

        requestFerdigstillJournalpost shouldBe null
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
        requestHarOppgave shouldBe null

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBe null
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
            requestHarOppgave.method shouldBe "GET"
            requestHarOppgave.url.queryParameter("statuskategori") shouldBe "AAPEN"
            requestHarOppgave.url.queryParameter("journalpostId") shouldBe PapirSoknadMedOrgNrPerson.JOURNALPOST_ID
        }

        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestOpprettOppgave.method shouldBe "POST"

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBe null

        val oppgaveRequestBody = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        oppgaveRequestBody.journalpostId shouldBe PapirSoknadMedOrgNrPerson.JOURNALPOST_ID
        oppgaveRequestBody.orgnr shouldBe PapirSoknadMedOrgNrPerson.ORGNR
        oppgaveRequestBody.aktoerId shouldBe null
        oppgaveRequestBody.tema shouldBe "SYK"
        oppgaveRequestBody.behandlingstema shouldBe "ab0434"
        oppgaveRequestBody.behandlingstype shouldBe null
    }

    @AfterAll
    fun `Kodeverket ble bare kalt en gang og resultatet ble chacet`() {
        val request = kodeverkMockWebServer.takeRequest(1, TimeUnit.SECONDS)!!
        request.url.encodedPath shouldBe "/api/v1/hierarki/TemaSkjemaGjelder/noder"

        kodeverkMockWebServer.requestCount shouldBe 1
    }
}
