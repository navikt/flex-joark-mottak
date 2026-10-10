package mock

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.ident.*
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import tools.jackson.module.kotlin.readValue

object PdlMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val graphReq: GraphQLRequest = objectMapper.readValue(request.body!!.utf8())
        val ident = graphReq.variables["ident"] ?: return MockResponse(code = 400, body = "Ingen ident variabel")

        if (ident.startsWith("2")) {
            return skapResponse(listOf(ident, ident.replaceFirstChar { "1" }))
        }
        if (ident.startsWith("3")) {
            return skapResponse(listOf(ident, ident.replaceFirstChar { "1" }, ident.replaceFirstChar { "2" }))
        }
        return skapResponse(listOf(ident))
    }

    fun skapResponse(identer: List<String>): MockResponse {
        val pdlIdenter =
            identer
                .map { PdlIdent(gruppe = FOLKEREGISTERIDENT, ident = it) }
                .toMutableList()
                .also { it.add(PdlIdent(gruppe = AKTORID, ident = identer.first() + "00")) }

        return MockResponse(
            body =
                GraphQLResponse(
                    data =
                        HentIdenterData(
                            hentIdenter =
                                HentIdenter(
                                    identer = pdlIdenter,
                                ),
                        ),
                    errors = null,
                ).serialisertTilString(),
        )
    }
}
