package no.nav.helse.flex.operations.eventenricher.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment
import no.nav.helse.flex.Environment.pdlClientid
import no.nav.helse.flex.infrastructure.MDCConstants
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
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
        "{\"query\":\"query(\$ident: ID!, \$grupper: [IdentGruppe!], \$historikk:Boolean = false){ hentIdenter(ident: \$ident, grupper:\$grupper, historikk:\$historikk){ identer{ident, historisk,gruppe}}}\"," +
            "\"variables\": {\"ident\":\"%s\",\"historikk\": false}}"

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

        return if (response.statusCode() == STATUS_OK) {
            try {
                val identer = gson.fromJson(response.body(), PdlResponse::class.java).identer
                identer!!.identer
            } catch (e: NullPointerException) {
                log.error("Klarer ikke hente ut bruker i responsen fra PDL på journalpost $journalpostId - ${e.message}")
                throw Exception("Klarer ikke hente ut bruker i responsen fra PDL på journalpost", e)
            }
        } else if (response.statusCode() == 404 || response.statusCode() == 503) {
            throw TemporarilyUnavailableException()
        } else {
            val errorMessage = gson.fromJson(response.body(), PdlErrorResponse::class.java).errors!![0].message
            log.error("Feil ved kall mot PDL på journalpost $journalpostId. Status: ${response.statusCode()} og feilmelding $errorMessage")
            throw ExternalServiceException(errorMessage!!, response.statusCode())
        }
    }

    data class GraphQLRequest(val query: String, val variables: Map<String, String>)

    private fun requestToJson(graphQLRequest: GraphQLRequest): String {
        return try {
            ObjectMapper().writeValueAsString(graphQLRequest)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
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
