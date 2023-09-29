package mock

import no.nav.helse.flex.felleskodeverk.Beskrivelse
import no.nav.helse.flex.felleskodeverk.Betydning
import no.nav.helse.flex.felleskodeverk.FkvKrutkoder
import no.nav.helse.flex.oppgave.OppgaveSearchResponse
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object KodeverkMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.requestUrl?.encodedPath) {
            "/api/v1/kodeverk/Krutkoder/koder/betydninger" -> MockResponse().setBody(FkvKrutkoder(mapOf(
                "NAV 08-07.04 D:SYK" to listOf(Betydning("1990", "2023", mapOf("nb" to Beskrivelse("TODO;NAV 08-07.04 D;ab0434;;", "TODO")))),
                "NAV 08-35.01:SYK" to listOf(Betydning("1990", "2023", mapOf("nb" to Beskrivelse("TODO;NAV 08-35.01;ae0004;;", "TODO"))))
            )).serialisertTilString())
            else -> MockResponse().setResponseCode(404)
                .setBody("Har ikke implemetert kodeverk mock api for ${request.requestUrl}")
        }
    }
}
