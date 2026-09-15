package mock

import mockwebserver3.MockResponse
import mockwebserver3.QueueDispatcher
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.oppgave.Oppgave
import no.nav.helse.flex.oppgave.OppgaveRequest
import no.nav.helse.flex.oppgave.OppgaveSearchResponse
import no.nav.helse.flex.serialisertTilString
import tools.jackson.module.kotlin.readValue

object OppgaveMockDispatcher : QueueDispatcher() {
    // Vi leser oppgaveRequest og må lagre den unna for å kunne lese den igjen
    val oppgaveRequestBodyListe = mutableListOf<OppgaveRequest>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.url.encodedPath != "/api/v1/oppgaver") {
            return MockResponse(
                code = 404,
                body = "Har ikke implemetert oppgave mock api for ${request.url}",
            )
        }

        if (request.headers["X-Correlation-ID"] == null) {
            return MockResponse(code = 400, body = "Påkrevd header mangler: X-Correlation-ID")
        }

        if (responseQueue.peek() != null) {
            return withContentTypeApplicationJson { responseQueue.take() }
        }

        when (request.method) {
            "GET" -> return withContentTypeApplicationJson {
                MockResponse(body = OppgaveSearchResponse().serialisertTilString())
            }
            "POST" -> {
                oppgaveRequestBodyListe.add(objectMapper.readValue<OppgaveRequest>(request.body!!.toByteArray()))
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
                    MockResponse(code = 201, body = oppgave.serialisertTilString())
                }
            }
            else -> return MockResponse(
                code = 404,
                body = "Har ikke implemetert oppgave mock api for metode ${request.method}",
            )
        }
    }
}
