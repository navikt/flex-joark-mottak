package mock

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.oppgave.Oppgave
import no.nav.helse.flex.oppgave.OppgaveRequest
import no.nav.helse.flex.oppgave.OppgaveSearchResponse
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest

object OppgaveMockDispatcher : QueueDispatcher() {
    // Vi leser oppgaveRequest og må lagre den unna for å kunne lese den igjen
    val oppgaveRequestBodyListe = mutableListOf<OppgaveRequest>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.requestUrl?.encodedPath != "/api/v1/oppgaver") {
            return MockResponse()
                .setResponseCode(404)
                .setBody("Har ikke implemetert oppgave mock api for ${request.requestUrl}")
        }

        if (request.headers["X-Correlation-ID"] == null) {
            return MockResponse().setResponseCode(400).setBody("Påkrevd header mangler: X-Correlation-ID")
        }

        if (responseQueue.peek() != null) {
            return withContentTypeApplicationJson { responseQueue.take() }
        }

        when (request.method) {
            "GET" -> return withContentTypeApplicationJson {
                MockResponse().setBody(OppgaveSearchResponse().serialisertTilString())
            }
            "POST" -> {
                oppgaveRequestBodyListe.add(objectMapper.readValue<OppgaveRequest>(request.body.readByteArray()))
                val requestBody = oppgaveRequestBodyListe.last()
                val oppgave =
                    Oppgave(
                        id = "123123",
                        beskrivelse = requestBody.beskrivelse,
                        oppgavetype = requestBody.oppgavetype,
                        tema = requestBody.tema,
                        tildeltEnhetsnr = requestBody.tildeltEnhetsnr ?: "4488",
                    )
                return withContentTypeApplicationJson {
                    MockResponse().setBody(oppgave.serialisertTilString()).setResponseCode(201)
                }
            }
            else -> return MockResponse()
                .setResponseCode(404)
                .setBody("Har ikke implemetert oppgave mock api for metode ${request.method}")
        }
    }
}
