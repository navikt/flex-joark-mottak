@file:Suppress("ktlint:standard:max-line-length")

package retry

import FellesTestOppsett
import com.fasterxml.jackson.module.kotlin.readValue
import mock.OppgaveMockDispatcher
import mock.PapirSoknadPerson
import no.nav.helse.flex.journalpost.FerdigstillJournalpostRequest
import no.nav.helse.flex.objectMapper
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class RebehandlingTest : FellesTestOppsett() {
    @Test
    fun `Papir sykepengesøknad legges over på retry topic, og blir rebehandlet`() {
        safMockWebserver.enqueue(MockResponse().setResponseCode(500))

        kafkaProducer.send(ProducerRecord(topic, PapirSoknadPerson.kafkaEvent)).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(20, TimeUnit.SECONDS)!!
        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestOppdaterJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!

        requestHarOppgave.requestLine shouldBeEqualTo "GET /api/v1/oppgaver?statuskategori=AAPEN&oppgavetype=JFR&oppgavetype=FDR&journalpostId=${PapirSoknadPerson.JOURNALPOST_ID} HTTP/1.1"

        requestOpprettOppgave.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
        val body = OppgaveMockDispatcher.oppgaveRequestBodyListe.last()
        body.journalpostId shouldBeEqualTo PapirSoknadPerson.JOURNALPOST_ID
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo "ab0434"
        body.behandlingstype shouldBeEqualTo null

        requestOppdaterJournalpost.method shouldBeEqualTo "PUT"
        requestOppdaterJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}"

        requestFerdigstillJournalpost.method shouldBeEqualTo "PATCH"
        requestFerdigstillJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.JOURNALPOST_ID}/ferdigstill"
        objectMapper.readValue<FerdigstillJournalpostRequest>(requestFerdigstillJournalpost.body.readUtf8()).journalfoerendeEnhet shouldBeEqualTo "9999"
    }
}
