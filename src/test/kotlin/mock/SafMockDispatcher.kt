package mock

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import mockwebserver3.MockResponse
import mockwebserver3.QueueDispatcher
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.SafClient
import no.nav.helse.flex.objectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

object SafMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.method != "POST") return MockResponse(code = 400, body = "Ingen saf")
        if (request.url.encodedPath != "/graphql") {
            return MockResponse(
                code = 404,
                body = "Har ikke implemetert saf mock api for ${request.url}",
            )
        }
        if (responseQueue.peek() != null) {
            return responseQueue.take()
        }

        val journalpostId = objectMapper.readValue<GraphQLRequest>(request.body!!.toByteArray()).variables["id"]

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
                MockResponse(
                    code = 404,
                    body = "Har ikke implemetert saf mock api for journalpostId $journalpostId",
                )
            }
        }
    }

    private val objectMapperWithVisibility =
        (objectMapper as JsonMapper)
            .rebuild()
            .changeDefaultVisibility { it.withFieldVisibility(Visibility.ANY) }
            .build()

    private fun response(journalpost: Journalpost) =
        MockResponse(
            body =
                objectMapperWithVisibility.writeValueAsString(
                    GraphQLResponse(
                        data = SafClient.ResponseData(journalpost),
                        errors = null,
                    ),
                ),
        )
}
