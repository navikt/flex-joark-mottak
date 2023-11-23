package mock

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.SafClient
import no.nav.helse.flex.objectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest

object SafMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.method != "POST") return MockResponse().setResponseCode(400).setBody("Ingen saf")
        if (request.requestUrl?.encodedPath != "/graphql") {
            return MockResponse().setResponseCode(404)
                .setBody("Har ikke implemetert saf mock api for ${request.requestUrl}")
        }
        if (responseQueue.peek() != null) {
            return responseQueue.take()
        }

        val journalpostId = objectMapper.readValue<GraphQLRequest>(request.body.readByteArray()).variables["id"]

        return when (journalpostId) {
            DigitalSoknadPerson.journalpostId -> response(DigitalSoknadPerson.journalpost)

            PapirSoknadPerson.journalpostId -> response(PapirSoknadPerson.journalpost)

            InntektsopplysningerPerson.journalpostId -> response(InntektsopplysningerPerson.journalpost)

            KlagePerson.journalpostId -> response(KlagePerson.journalpost)

            UtlanskPerson.journalpostId -> response(UtlanskPerson.journalpost)

            BrevløsPerson.journalpostId -> response(BrevløsPerson.journalpost)

            UkjentBrevkodePerson.journalpostId -> response(UkjentBrevkodePerson.journalpost)

            JournalpostUtenPerson.journalpostId -> response(JournalpostUtenPerson.journalpost)

            InntektsmeldingPerson.journalpostId -> response(InntektsmeldingPerson.journalpost)

            else -> {
                MockResponse().setResponseCode(404)
                    .setBody("Har ikke implemetert saf mock api for journalpostId $journalpostId")
            }
        }
    }

    private val objectMapperWithVisibility = objectMapper.copy().setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

    private fun response(journalpost: Journalpost) = MockResponse().setBody(
        objectMapperWithVisibility.writeValueAsString(
            GraphQLResponse(
                data = SafClient.ResponseData(journalpost),
                errors = null
            )
        )
    )
}
