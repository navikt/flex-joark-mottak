package no.nav.helse.flex.operations.eventenricher.pdl

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

class PdlClient {
    private val persondataUrl: String = Environment.persondataUrl
    private val gson: Gson
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String?>>
    private val azureAdClient: AzureAdClient
    private val query = """{
        "query":"query(${"$"}ident: ID!, ${"$"}grupper: [IdentGruppe!], ${"$"}historikk:Boolean = false){ 
            hentIdenter(ident: ${"$"}ident, grupper:${"$"}grupper, historikk:${"$"}historikk){ 
                identer{ident, historisk, gruppe}
            }
        }",
        "variables": {"ident":"%s","historikk": false}}
    """.trimIndent()

    init {
        val pdlClientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(pdlClientFunction)
        azureAdClient = AzureAdClient(pdlClientid)
        gson = Gson()
    }

    @Throws(Exception::class)
    private fun excecute(req: HttpRequest): HttpResponse<String?> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    @Throws(Exception::class)
    fun retrieveIdenterFromPDL(fnr: String, tema: String?, journalpostId: String?): List<Ident> {
        val correlationId = MDC.get(MDCConstants.CORRELATION_ID)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(persondataUrl))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, correlationId)
            .header(TEMA_HEADER, tema)
            .POST(HttpRequest.BodyPublishers.ofString(persondataBody(query, fnr)))
            .build()
        val response = resilience.execute(request)

        return if (response.statusCode() == STATUS_OK) {
            try {
                val identer = gson.fromJson(response.body(), PdlResponse::class.java).identer
                identer!!.identer
            } catch (e: NullPointerException) {
                log.error(
                    "Klarer ikke hente ut bruker i responsen fra PDL på journalpost {} - {}",
                    journalpostId,
                    e.message
                )
                throw Exception("Klarer ikke hente ut bruker i responsen fra PDL på journalpost", e)
            }
        } else if (response.statusCode() == 404 || response.statusCode() == 503) {
            throw TemporarilyUnavailableException()
        } else {
            val errorMessage = gson.fromJson(response.body(), PdlErrorResponse::class.java).errors!![0].message
            log.error(
                "Feil ved kall mot PDL på journalpost {}. Status: {} og feilmelding {}",
                journalpostId,
                response.statusCode(),
                errorMessage
            )
            throw ExternalServiceException(errorMessage!!, response.statusCode())
        }
    }

    private fun persondataBody(query: String, fnr: String): String {
        return String.format(query, fnr)
    }

    companion object {
        private val log = LoggerFactory.getLogger(PdlClient::class.java)
        private const val CORRELATION_HEADER = "X-Correlation-ID"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val TEMA_HEADER = "tema"
        private const val STATUS_OK = 200
    }
}
