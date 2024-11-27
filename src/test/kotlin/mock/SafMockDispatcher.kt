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
            DigitalSoknadPerson.JOURNALPOST_ID -> response(DigitalSoknadPerson.journalpost)

            PapirSoknadPerson.JOURNALPOST_ID -> response(PapirSoknadPerson.journalpost)

            InntektsopplysningerPerson.JOURNALPOST_ID -> response(InntektsopplysningerPerson.journalpost)

            KlagePerson.JOURNALPOST_ID -> response(KlagePerson.journalpost)

            UtenlandskPerson.JOURNALPOST_ID -> response(UtenlandskPerson.journalpost)

            BrevløsPerson.JOURNALPOST_ID -> response(BrevløsPerson.journalpost)

            UkjentBrevkodePerson.JOURNALPOST_ID -> response(UkjentBrevkodePerson.journalpost)

            JournalpostUtenPerson.JOURNALPOST_ID -> response(JournalpostUtenPerson.journalpost)

            InntektsmeldingPerson.JOURNALPOST_ID -> response(InntektsmeldingPerson.journalpost)

            PapirSoknadMedOrgNrPerson.JOURNALPOST_ID -> response(PapirSoknadMedOrgNrPerson.journalpost)

            else -> {
                MockResponse().setResponseCode(404)
                    .setBody("Har ikke implemetert saf mock api for journalpostId $journalpostId")
            }
        }
    }

    private val objectMapperWithVisibility = objectMapper.copy().setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

    private fun response(journalpost: Journalpost) =
        MockResponse().setBody(
            objectMapperWithVisibility.writeValueAsString(
                GraphQLResponse(
                    data = SafClient.ResponseData(journalpost),
                    errors = null,
                ),
            ),
        )
}
