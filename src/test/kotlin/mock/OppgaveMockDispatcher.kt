package mock

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.oppgave.Oppgave
import no.nav.helse.flex.oppgave.OppgaveRequest
import no.nav.helse.flex.oppgave.OppgaveSearchResponse
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object OppgaveMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.requestUrl?.encodedPath != "/api/v1/oppgaver") return MockResponse().setResponseCode(404)
            .setBody("Har ikke implemetert oppgave mock api for ${request.requestUrl}")

        return when (request.method) {
            "GET" -> MockResponse().setBody(OppgaveSearchResponse().serialisertTilString())
            "POST" -> {
                val oppgaveRequest = objectMapper.readValue<OppgaveRequest>(request.body.readByteArray())
                val oppgave = Oppgave(id = "123123", beskrivelse = oppgaveRequest.beskrivelse, oppgavetype = oppgaveRequest.oppgavetype, tema = oppgaveRequest.tema, tildeltEnhetsnr = oppgaveRequest.tildeltEnhetsnr)
                MockResponse().setBody(oppgave.serialisertTilString()).setResponseCode(201)
            }
            else -> MockResponse().setResponseCode(404)
                .setBody("Har ikke implemetert oppgave mock api for metode ${request.method}")
        }
    }
}
