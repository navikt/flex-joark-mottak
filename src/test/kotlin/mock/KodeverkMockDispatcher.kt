package mock

import no.nav.helse.flex.felleskodeverk.Beskrivelse
import no.nav.helse.flex.felleskodeverk.Betydning
import no.nav.helse.flex.felleskodeverk.FkvKrutkoder
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object KodeverkMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.requestUrl?.encodedPath) {
            "/api/v1/kodeverk/Krutkoder/koder/betydninger" -> MockResponse().setBody(
                FkvKrutkoder(
                    mapOf(
                        "NAV 08-07.04 D:SYK" to listOf(Betydning("2021-06-01", "9999-12-31", mapOf("nb" to Beskrivelse("Sykmelding D;NAV 08-07.04 D;ab0434;", "Sykmelding D;NAV 08-07.04 D;ab0434;")))),
                        "NAV 08-35.01:SYK" to listOf(Betydning("2021-06-01", "9999-12-31", mapOf("nb" to Beskrivelse("Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger;NAV 08-35.01;;ae0004", "Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger;NAV 08-35.01;;ae0004")))),
                        "NAV 90-00.08 K:SYK" to listOf(Betydning("2021-06-01", "9999-12-31", mapOf("nb" to Beskrivelse("Klage;NAV 90-00.08 K;;", "Klage;NAV 90-00.08 K;;")))),
                        "NAV 08-09.06:SYK" to listOf(Betydning("2021-06-01", "9999-12-31", mapOf("nb" to Beskrivelse("Egenerklæring for utenlandske sykmeldinger;NAV 08-09.06;;ae0106", "Egenerklæring for utenlandske sykmeldinger;NAV 08-09.06;;ae0106"))))
                    )
                ).serialisertTilString()
            )
            else -> MockResponse().setResponseCode(404)
                .setBody("Har ikke implemetert kodeverk mock api for ${request.requestUrl}")
        }
    }
}
