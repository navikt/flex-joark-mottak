package no.nav.helse.flex.journalpost

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.refactor.ExternalServiceException
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class SafClient(
    @Value("\${SAF_URL}")
    private val safApiUrl: String,
    private val safRestTemplate: RestTemplate
) {
    private val log = logger()

    fun hentJournalpost(journalpostId: String): Journalpost {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(safApiUrl)
            .path("/graphql")
            .build()
            .toUri()

        val responseEntity = safRestTemplate.exchange(
            uri,
            HttpMethod.POST,
            HttpEntity(
                GraphQLRequest(SAF_QUERY_FIND_JOURNALPOST, mapOf("id" to journalpostId)).serialisertTilString(),
                headers
            ),
            String::class.java
        )

        val parsedResponse = responseEntity.body?.let { objectMapper.readValue<GraphQLResponse<ResponseData>>(it) }

        if (responseEntity.statusCode.is2xxSuccessful && parsedResponse != null) {
            return parsedResponse.data.journalpost
        }

        // TODO: Resttemplate kaster exception når status ikke er 2xx-ok
        val errorMessage = parsedResponse?.hentErrors()
        log.error("Feil ved kall mot PDL på journalpost $journalpostId. Status: ${responseEntity.statusCode.value()} og feilmelding $errorMessage")
        throw ExternalServiceException(errorMessage!!, responseEntity.statusCode.value())
    }

    private data class ResponseData(
        val journalpost: Journalpost
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
