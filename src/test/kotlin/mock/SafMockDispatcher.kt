package mock

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.SafClient
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import no.nav.security.mock.oauth2.extensions.endsWith
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object SafMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.method != "POST") return MockResponse().setResponseCode(400).setBody("Ingen saf")
        if (request.requestUrl?.encodedPath != "/graphql") return MockResponse().setResponseCode(404)
            .setBody("Har ikke implemetert saf mock api for ${request.requestUrl}")

        val journalpostId = objectMapper.readValue<GraphQLRequest>(request.body.readByteArray()).variables["id"]

        return when (journalpostId) {
            DigitalSoknadPerson.journalpostId ->
                response(DigitalSoknadPerson.journalpost)

            PapirSoknadPerson.journalpostId ->
                response(PapirSoknadPerson.journalpost)

            InntektsopplysningerPerson.journalpostId ->
                response(InntektsopplysningerPerson.journalpost)

            InntektsmeldingPerson.journalpostId ->
                response(InntektsmeldingPerson.journalpost)

            else -> {
                MockResponse().setResponseCode(404)
                    .setBody("Har ikke implemetert saf mock api for journalpostId $journalpostId")
            }
        }
    }

    private fun response(journalpost: Journalpost) = MockResponse().setBody(
        GraphQLResponse(
            data = SafClient.ResponseData(journalpost),
            errors = null
        ).serialisertTilString()
    )
}
