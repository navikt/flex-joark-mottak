package no.nav.helse.flex.operations.eventenricher.saf

import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment
import no.nav.helse.flex.infrastructure.MDCConstants
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse

class SafClient {
    private val safUrl: String = Environment.safUrl
    private val azureAdClient: AzureAdClient
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String?>>

    init {
        val safClientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(safClientFunction)
        azureAdClient = AzureAdClient(Environment.safClientId)
    }

    private fun excecute(req: HttpRequest): HttpResponse<String?> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    fun retriveJournalpost(journalpostId: String): Journalpost {
        val correlationId = MDC.get(MDCConstants.CORRELATION_ID)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(safUrl))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, correlationId)
            .header(X_XSRF_TOKEN, correlationId)
            .header("cookie", XSRF_TOKEN + "=" + correlationId)
            .POST(BodyPublishers.ofString(journalpostBody(journalpostId)))
            .build()
        val response = resilience.execute(request)

        if (response.statusCode() == STATUS_OK) {
            return objectMapper.readValue(response.body(), SafResponse::class.java)
                .data
                .journalpost
        }
        if (response.statusCode() == NOT_AVAILABLE) {
            log.info("journalposten $journalpostId : JournalpostApi returnerte 404, tjeneste ikke tigjengelig.")
            throw TemporarilyUnavailableException()
        }
        else {
            val safErrorMessage = objectMapper.readValue(response.body(), SafErrorMessage::class.java)
            log.error("Ved behandling av Journalpost $journalpostId: Feil (${safErrorMessage.status}) $SERVICENAME_SAF; error: ${safErrorMessage.error}, message: ${safErrorMessage.message}",)
            throw ExternalServiceException(safErrorMessage.message!!, safErrorMessage.status)
        }
    }

    private fun journalpostBody(journalpostId: String): String {
        val query = String.format(SAF_JOURNALPOST_QUERY, journalpostId)
        val body = JSONObject()
        body.put(QUERY_TAG, query)
        return body.toString()
    }

    companion object {
        private val log = LoggerFactory.getLogger(SafClient::class.java)
        private const val SAF_JOURNALPOST_QUERY = "{journalpost(journalpostId: \"%s\") {" +
            " tittel" +
            " journalforendeEnhet" +
            " journalpostId" +
            " avsenderMottaker {" +
            " id" +
            " type" +
            " }" +
            " journalstatus" +
            " tema" +
            " behandlingstema" +
            " bruker {" +
            "   id" +
            "   type" +
            " }" +
            " relevanteDatoer {" +
            "   dato" +
            "   datotype" +
            " }" +
            " dokumenter {" +
            "   brevkode" +
            "   tittel" +
            "   dokumentInfoId" +
            " }" +
            "}}"
        private const val CORRELATION_HEADER = "X-Correlation-ID"
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
        private const val X_XSRF_TOKEN = "X-XSRF-TOKEN"
        private const val XSRF_TOKEN = "XSRF-TOKEN"
        private const val QUERY_TAG = "query"
        private const val SERVICENAME_SAF = "Saf"
        private const val STATUS_OK = 200
        private const val NOT_AVAILABLE = 404
    }
}
