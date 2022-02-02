package no.nav.helse.flex.operations.generell.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment
import no.nav.helse.flex.Environment.oppgaveClientId
import no.nav.helse.flex.infrastructure.MDCConstants.CORRELATION_ID
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incCreateOppgaveCounter
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
import no.nav.helse.flex.objectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OppgaveClient {
    private val CONTENT_TYPE_HEADER = "Content-Type"
    private val AUTHORIZATION_HEADER = "Authorization"
    private val CORRELATION_HEADER = "X-Correlation-ID"
    private val PARAM_STATUSKATEGORI_AAPEN = "AAPEN"
    private val PARAM_OPPGAVETYPE_JFR = "JFR"
    private val PARAM_OPPGAVETYPE_FDR = "FDR"
    private val STATUS_OK = 200
    private val log = LoggerFactory.getLogger(OppgaveClient::class.java)

    private val oppgaveUrl: String = Environment.oppgaveUrl
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String>>
    private val azureAdClient: AzureAdClient

    init {
        val clientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(clientFunction)
        azureAdClient = AzureAdClient(oppgaveClientId)
    }

    fun createOppgave(requestData: CreateOppgaveData): Oppgave {
        val correlationId = MDC.get(CORRELATION_ID)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$oppgaveUrl/api/v1/oppgaver"))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, correlationId)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestData)))
            .build()
        val response = resilience.execute(request)

        if (response.statusCode() == 201) {
            val oppgave: Oppgave = objectMapper.readValue(response.body())
            incCreateOppgaveCounter(oppgave.oppgavetype, oppgave.tema)
            return oppgave
        } else if (response.statusCode() == 404) {
            log.error("Klarte ikke opprette oppgave på journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode()}")
            throw TemporarilyUnavailableException()
        } else if (response.statusCode() >= 500) {
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke opprette oppgave på journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode()}. $errorText")
            throw TemporarilyUnavailableException()
        } else {
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke opprette oppgave på journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode()}. $errorText")
            throw ExternalServiceException(errorText, response.statusCode())
        }
    }

    fun updateOppgave(enrichedKafkaEvent: EnrichedKafkaEvent): Oppgave {
        val correlationId = MDC.get(CORRELATION_ID)
        val oppgave = enrichedKafkaEvent.oppgave
        val request = HttpRequest.newBuilder()
            .uri(URI.create(oppgaveUrl + "/api/v1/oppgaver/" + oppgave!!.id))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, correlationId)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(oppgave)))
            .build()
        val response = resilience.execute(request)

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body())
        } else if (response.statusCode() == 404) {
            log.error("Klarte ikke oppdatere oppgave ${oppgave.id} på journalpost ${enrichedKafkaEvent.journalpostId}, statuskode: ${response.statusCode()}")
            throw TemporarilyUnavailableException()
        } else if (response.statusCode() >= 500) {
            log.error("5XX fra oppgave $response ${response.body()} ${response.body()}")
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke oppdatere oppgave ${oppgave.id} på journalpost ${enrichedKafkaEvent.journalpostId}, statuskode: ${response.statusCode()}. $errorText")
            throw TemporarilyUnavailableException()
        } else {
            log.error("Annen feil fra oppgave $response ${response.body()} ${response.body()}")
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke oppdatere oppgave ${oppgave.id} på journalpost ${enrichedKafkaEvent.journalpostId}, statuskode: ${response.statusCode()}. $errorText")
            throw ExternalServiceException("Feil under oppdatering av oppgave", response.statusCode())
        }
    }

    fun checkIfJournapostHasOppgave(journalpostId: String): Boolean {
        val request = HttpRequest.newBuilder()
            .uri(buildUri(journalpostId))
            .header(CONTENT_TYPE_HEADER, "application/json")
            .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
            .header(CORRELATION_HEADER, MDC.get(CORRELATION_ID))
            .GET()
            .build()
        val response = resilience.execute(request)

        if (response.statusCode() == STATUS_OK) {
            return objectMapper
                .readValue<OppgaveSearchResponse>(response.body())
                .harTilknyttetOppgave()
        } else if (response.statusCode() == 404 || response.statusCode() == 503) {
            throw TemporarilyUnavailableException()
        } else {
            val error: OppgaveErrorResponse = objectMapper.readValue(response.body())
            throw ExternalServiceException(error.feilmelding, response.statusCode())
        }
    }

    fun buildUri(journalpostId: String): URI {
        return URI.create("$oppgaveUrl/api/v1/oppgaver?statuskategori=$PARAM_STATUSKATEGORI_AAPEN&oppgavetype=$PARAM_OPPGAVETYPE_JFR&oppgavetype=$PARAM_OPPGAVETYPE_FDR&journalpostId=$journalpostId")
    }

    private fun excecute(req: HttpRequest): HttpResponse<String> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }

    private fun hentFeilmelding(response: HttpResponse<String>): String {
        val oppgaveErrorResponse: OppgaveErrorResponse = objectMapper.readValue(response.body())
        return oppgaveErrorResponse.feilmelding
    }
}
