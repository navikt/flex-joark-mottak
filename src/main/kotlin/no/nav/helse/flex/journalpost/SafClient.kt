package no.nav.helse.flex.journalpost

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.lang.Exception

@Component
class SafClient(
    @Value("\${SAF_URL}")
    private val safApiUrl: String,
    private val safRestTemplate: RestTemplate,
) {
    private val log = logger()

    fun hentJournalpost(journalpostId: String): Journalpost {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val responseEntity =
            safRestTemplate.exchange(
                "$safApiUrl/graphql",
                HttpMethod.POST,
                HttpEntity(
                    GraphQLRequest(SAF_QUERY_FIND_JOURNALPOST, mapOf("id" to journalpostId)).serialisertTilString(),
                    headers,
                ),
                String::class.java,
            )

        if (responseEntity.body == null) {
            throw Exception(
                "Mangler body i response fra saf for journalpost $journalpostId, statuskode: ${responseEntity.statusCode.value()}",
            )
        }

        val parsedResponse = responseEntity.body!!.let { objectMapper.readValue<GraphQLResponse<ResponseData>>(it) }

        parsedResponse.errors?.forEach {
            log.error("Feil i response fra saf for journalpost $journalpostId", it.serialisertTilString())
        }

        if (!responseEntity.statusCode.is2xxSuccessful) {
            // Tror denne aldri kan skje
            throw Exception("Saf response for journalpost $journalpostId med statuskode ${responseEntity.statusCode.value()}")
        }

        return parsedResponse.data.journalpost
    }

    data class ResponseData(
        val journalpost: Journalpost,
    )
}

private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val SAF_QUERY_FIND_JOURNALPOST = """
query FindJournalpost(${"$"}id: String!) {
    journalpost(journalpostId: ${"$"}id) {
        journalpostId
        journalstatus
        tittel
        journalforendeEnhet
        tema
        behandlingstema
        bruker {
            id
            type
        }
        avsenderMottaker {
            id 
            type 
            navn 
            land
        }
        relevanteDatoer {
            dato
            datotype
        }
        dokumenter {
            brevkode
            tittel
            dokumentInfoId
        }
    }
}
"""
