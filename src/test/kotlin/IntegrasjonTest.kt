import com.fasterxml.jackson.module.kotlin.readValue
import mock.DigitalSoknadPerson
import mock.InntektsmeldingPerson
import mock.InntektsopplysningerPerson
import mock.PapirSoknadPerson
import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.oppgave.OppgaveRequest
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.awaitility.Awaitility
import util.fromKClassToGenericRecord
import java.util.concurrent.TimeUnit

class IntegrasjonTest : BaseTestClass() {
    @Test
    fun `Mottar journalpost som allerede er ferdig arkivert i sykepengesoknad-arkivering-oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic, fromKClassToGenericRecord(DigitalSoknadPerson.kafkaEvent)
            )
        ).get()

        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS) shouldBeEqualTo null
    }

    @Test
    fun `Mottar journalpost som skal journalføres`() {
        kafkaProducer.send(
            ProducerRecord(
                topic, fromKClassToGenericRecord(PapirSoknadPerson.kafkaEvent)
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo PapirSoknadPerson.journalpostId

        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestOpprettOppgave.method shouldBeEqualTo "POST"
        /*
        val body = objectMapper.readValue<OppgaveRequest>(requestOpprettOppgave.body.readUtf8())
        body.journalpostId shouldBeEqualTo PapirSoknadPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo "ab0434"
        body.behandlingstype shouldBeEqualTo ""
        */

        val requestOppdaterJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestOppdaterJournalpost.method shouldBeEqualTo "PUT"
        requestOppdaterJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.journalpostId}"

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestFerdigstillJournalpost.method shouldBeEqualTo "PATCH"
        requestFerdigstillJournalpost.requestUrl?.encodedPath shouldBeEqualTo "/rest/journalpostapi/v1/journalpost/${PapirSoknadPerson.journalpostId}/ferdigstill"
    }

    @Test
    fun `Mottar journalpost som skal ha JFR oppgave`() {
        kafkaProducer.send(
            ProducerRecord(
                topic, fromKClassToGenericRecord(InntektsopplysningerPerson.kafkaEvent)
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestHarOppgave.method shouldBeEqualTo "GET"
        requestHarOppgave.requestUrl?.queryParameter("statuskategori") shouldBeEqualTo "AAPEN"
        requestHarOppgave.requestUrl?.queryParameter("journalpostId") shouldBeEqualTo InntektsopplysningerPerson.journalpostId

        val requestOpprettOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!
        requestOpprettOppgave.method shouldBeEqualTo "POST"
        /*
        val body = objectMapper.readValue<OppgaveRequest>(requestOpprettOppgave.body.readUtf8())
        body.journalpostId shouldBeEqualTo PapirSoknadPerson.journalpostId
        body.tema shouldBeEqualTo "SYK"
        body.behandlingstema shouldBeEqualTo "ae0004"
        body.behandlingstype shouldBeEqualTo ""
        */

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Mottar journalpost som vi skal ingnorere`() {
        kafkaProducer.send(
            ProducerRecord(
                topic, fromKClassToGenericRecord(InntektsmeldingPerson.kafkaEvent)
            )
        ).get()

        val requestHarOppgave = oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestHarOppgave shouldBeEqualTo null

        val requestFerdigstillJournalpost = dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)
        requestFerdigstillJournalpost shouldBeEqualTo null
    }

    @Test
    fun `Kodeverket ble bare kalt en gang og resultatet ble chacet`() {
        val request = kodeverkMockWebServer.takeRequest(1, TimeUnit.SECONDS)!!
        request.requestUrl?.encodedPath shouldBeEqualTo "/api/v1/kodeverk/Krutkoder/koder/betydninger"

        kodeverkMockWebServer.requestCount shouldBeEqualTo 1
    }
}
