@file:Suppress("ktlint:standard:max-line-length")

package retry

import FellesTestOppsett
import io.kotest.matchers.shouldBe
import mock.OppgaveMockDispatcher
import mock.PapirSoknadPerson
import mockwebserver3.MockResponse
import no.nav.helse.flex.journalpost.FerdigstillJournalpostRequest
import no.nav.helse.flex.objectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.TimeUnit

class RebehandlingTest : FellesTestOppsett() {
    @Test
    fun `Papir sykepengesøknad legges over på retry topic, og blir rebehandlet`() {
        safMockWebserver.enqueue(MockResponse(code = 500))

        kafkaProducer.send(ProducerRecord(topic, PapirSoknadPerson.kafkaEvent)).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(20, TimeUnit.SECONDS)!!
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
}
