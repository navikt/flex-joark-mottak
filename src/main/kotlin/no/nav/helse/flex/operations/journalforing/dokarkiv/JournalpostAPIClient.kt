package no.nav.helse.flex.operations.journalforing.dokarkiv

import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment.dokarkivClientId
import no.nav.helse.flex.infrastructure.MDCConstants
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class JournalpostAPIClient {
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val AUTHORIZATION_HEADER = "Authorization"
    private val CORRELATION_HEADER = "X-Correlation-ID"

    private val log = LoggerFactory.getLogger(JournalpostAPIClient::class.java)
    private val journalpostApiUrl: String = no.nav.helse.flex.Environment.journalpostApiUrl
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String?>>
    private val azureAdClient: AzureAdClient

    init {
        val clientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(clientFunction)
        azureAdClient = AzureAdClient(dokarkivClientId)
    }

    fun finalizeJournalpost(journalpostId: String) {
        val correlationId = MDC.get(MDCConstants.CORRELATION_ID)
        val requestbody = JSONObject().put("journalfoerendeEnhet", "9999")
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$journalpostApiUrl/$journalpostId/ferdigstill"))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestbody.toString()))
                .build()
            val response = resilience.execute(request)

            if (response.statusCode() == 200) {
                // ok
            } else if (response.statusCode() == 503 || response.statusCode() == 404) {
                log.error("Feilkode mot Journalpost API ${response.statusCode()}")
                throw TemporarilyUnavailableException()
            } else {
                log.error("Feilkode fra JournalpostApi: ${response.statusCode()} - ferdigstill body - ${response.body()}")
                throw ExternalServiceException(
                    "Kunne ikke ferdigstille Journalpsot",
                    response.statusCode()
                )
            }
        } catch (e: Exception) {
            log.error("Ukjent feil mot ferdigstill journalpost på journalpost {}", journalpostId, e)
            throw TemporarilyUnavailableException()
        }
    }

    fun updateJournalpost(journalpost: Journalpost) {
        val correlationId = MDC.get(MDCConstants.CORRELATION_ID)
        val journalpostId = journalpost.journalpostId
        val jsonBody = journalpost.toJson()
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$journalpostApiUrl/$journalpostId"))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build()
            val response = resilience.execute(request)

            if (response.statusCode() == 200) {
                // ok
            } else if (response.statusCode() == 503 || response.statusCode() == 404) {
                throw TemporarilyUnavailableException()
            } else {
                log.error("Feilkode fra JournalpostApi: ${response.statusCode()} ${response.body()} - oppdater")
                throw ExternalServiceException("Kunne ikke oppdatere Journalpsot", response.statusCode())
            }
        } catch (e: Exception) {
            log.error("Ukjent feil mot oppdater journalpost på journalpost ${journalpost.journalpostId}", e)
            throw TemporarilyUnavailableException()
        }
    }

    private fun excecute(req: HttpRequest): HttpResponse<String?> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }
}
