package mock

import no.nav.helse.flex.felleskodeverk.KodeverkBrevkoder
import no.nav.helse.flex.felleskodeverk.KodeverkBrevkoder.*
import no.nav.helse.flex.felleskodeverk.KodeverkBrevkoder.TemaNode.BrevkodeNode
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object KodeverkMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse =
        when (request.requestUrl?.encodedPath) {
            "/api/v1/hierarki/TemaSkjemaGjelder/noder" ->
                MockResponse().setBody(
                    KodeverkBrevkoder(
                        hierarkinivaaer = listOf("Tema", "TemaSkjemaGjelderverdier"),
                        noder =
                            mapOf(
                                "SYK" to
                                    TemaNode(
                                        kode = "SYK",
                                        undernoder =
                                            mapOf(
                                                "NAVe 08-07.11" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-07.11",
                                                        termer = mapOf("nb" to "ab0421;"),
                                                    ),
                                                "NAV 90-00.08" to
                                                    BrevkodeNode(
                                                        kode = "NAV 90-00.08",
                                                        termer = mapOf("nb" to ";ae0058"),
                                                    ),
                                                "NAV 08-20.20" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-20.20",
                                                        termer = mapOf("nb" to "ab0200;"),
                                                    ),
                                                "NAVe 08-09.06" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-09.06",
                                                        termer = mapOf("nb" to ";ae0106"),
                                                    ),
                                                "NAVe 08-09.07" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-09.07",
                                                        termer = mapOf("nb" to "ab0314;"),
                                                    ),
                                                "NAVe 08-09.08" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-09.08",
                                                        termer = mapOf("nb" to ";ae0237"),
                                                    ),
                                                "NAV 08-09.06" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-09.06",
                                                        termer = mapOf("nb" to ";ae0106"),
                                                    ),
                                                "NAV 08-47.05" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-47.05",
                                                        termer = mapOf("nb" to ";ae0004"),
                                                    ),
                                                "NAVe 08-14.01" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-14.01",
                                                        termer = mapOf("nb" to "ab0237;ae0121"),
                                                    ),
                                                "NAV 08-20.12" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-20.12",
                                                        termer = mapOf("nb" to "ab0200;ae0121"),
                                                    ),
                                                "NAV 08-09.07" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-09.07",
                                                        termer = mapOf("nb" to "ab0314;"),
                                                    ),
                                                "NAV 08-09.08" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-09.08",
                                                        termer = mapOf("nb" to ";ae0237"),
                                                    ),
                                                "NAV 08-20.05" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-20.05",
                                                        termer = mapOf("nb" to "ab0200;"),
                                                    ),
                                                "NAVe 08-35.01" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-35.01",
                                                        termer = mapOf("nb" to ";ae0004"),
                                                    ),
                                                "NAV 08-14.01" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-14.01",
                                                        termer = mapOf("nb" to "ab0237;ae0121"),
                                                    ),
                                                "NAVe 08-47.05" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-47.05",
                                                        termer = mapOf("nb" to ";ae0004"),
                                                    ),
                                                "NAVe 08-20.12" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-20.12",
                                                        termer = mapOf("nb" to "ab0200;ae0121"),
                                                    ),
                                                "NAV 08-07.04 D" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-07.04 D",
                                                        termer = mapOf("nb" to "ab0434;"),
                                                    ),
                                                "NAV 08-07.11" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-07.11",
                                                        termer = mapOf("nb" to "ab0421;"),
                                                    ),
                                                "NAV 08-35.01" to
                                                    BrevkodeNode(
                                                        kode = "NAV 08-35.01",
                                                        termer = mapOf("nb" to ";ae0004"),
                                                    ),
                                                "NAVe 08-20.05" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-20.05",
                                                        termer = mapOf("nb" to "ab0200;"),
                                                    ),
                                                "NAVe 08-20.20" to
                                                    BrevkodeNode(
                                                        kode = "NAVe 08-20.20",
                                                        termer = mapOf("nb" to "ab0200;"),
                                                    ),
                                            ),
                                    ),
                            ),
                    ).serialisertTilString(),
                )
            else ->
                MockResponse()
                    .setResponseCode(404)
                    .setBody("Har ikke implemetert kodeverk mock api for ${request.requestUrl}")
        }
}
