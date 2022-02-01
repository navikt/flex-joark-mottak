package no.nav.helse.flex.operations.eventenricher.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment
import no.nav.helse.flex.Environment.pdlClientid
import no.nav.helse.flex.infrastructure.MDCConstants
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
import no.nav.helse.flex.objectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

class PdlClient {
    private val persondataUrl: String = Environment.persondataUrl
    private val gson: Gson
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String?>>
    private val azureAdClient: AzureAdClient
    private val HENT_PERSON_QUERY =
        """
query(${"$"}ident: ID!){
  hentIdenter(ident: ${"$"}ident, historikk: false) {
    identer {
      ident,
      gruppe
    }
  }
}
"""

    init {
        val pdlClientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(pdlClientFunction)
        azureAdClient = AzureAdClient(pdlClientid)
        gson = Gson()
    }

    private fun excecute(req: HttpRequest): HttpResponse<String?> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    fun retrieveIdenterFromPDL(fnr: String, tema: String?, journalpostId: String?): List<Ident> {
        val correlationId = MDC.get(MDCConstants.CORRELATION_ID)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(persondataUrl))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, correlationId)
            .header(TEMA_HEADER, tema)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    requestToJson(
                        GraphQLRequest(
                            query = HENT_PERSON_QUERY,
                            variables = Collections.singletonMap(IDENT, fnr)
                        )
                    )
                )
            )
            .build()
        val response = resilience.execute(request)

        if (response.statusCode() == 404 || response.statusCode() == 503) {
            throw TemporarilyUnavailableException()
        }

        val parsedResponse: GetPersonResponse? = response.body()?.let {
            objectMapper.readValue(it)
        }

        if (response.statusCode() == STATUS_OK) {
            val identer = parsedResponse?.data?.hentIdenter?.identer

            if (identer != null) {
                return identer
            } else {
                log.error("Klarer ikke hente ut bruker i responsen fra PDL på journalpost $journalpostId - ${parsedResponse.hentErrors()}")
                throw Exception("Klarer ikke hente ut bruker i responsen fra PDL på journalpost")
            }
        }

        val errorMessage = parsedResponse.hentErrors()
        log.error("Feil ved kall mot PDL på journalpost $journalpostId. Status: ${response.statusCode()} og feilmelding $errorMessage")
        throw ExternalServiceException(errorMessage!!, response.statusCode())
    }

    private fun requestToJson(graphQLRequest: GraphQLRequest): String {
        return try {
            ObjectMapper().writeValueAsString(graphQLRequest)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    data class GraphQLRequest(val query: String, val variables: Map<String, String>)

    private fun GetPersonResponse?.hentErrors(): String? {
        return this?.errors?.map { it.message }?.joinToString(" - ")
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlClient::class.java)
        private const val CORRELATION_HEADER = "X-Correlation-ID"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val TEMA_HEADER = "tema"
        private const val STATUS_OK = 200
        private const val IDENT = "ident"
    }
}
