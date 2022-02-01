package no.nav.helse.flex.operations.generell.felleskodeverk

import com.fasterxml.jackson.module.kotlin.readValue
import io.vavr.CheckedFunction1
import no.nav.helse.flex.Environment.fkvUrl
import no.nav.helse.flex.Environment.proxyClientid
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.resilience.Resilience
import no.nav.helse.flex.infrastructure.security.AzureAdClient
import no.nav.helse.flex.objectMapper
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.naming.ServiceUnavailableException

class FkvClient {
    private val CORRELATION_HEADER = "Nav-Call-Id"
    private val NAV_CONSUMER_ID = "Nav-Consumer-Id"
    private val AUTHORIZATION_HEADER = "Authorization"
    private val log = LoggerFactory.getLogger(FkvClient::class.java)

    private val fellesKodeverkUrl = fkvUrl
    private val client = HttpClient.newHttpClient()
    private val resilience: Resilience<HttpRequest, HttpResponse<String>>
    private val azureAdClient: AzureAdClient

    init {
        val fkvClientFunction = CheckedFunction1 { req: HttpRequest -> excecute(req) }
        resilience = Resilience(fkvClientFunction)
        azureAdClient = AzureAdClient(proxyClientid)
    }

    fun fetchKrutKoder(): FkvKrutkoder {
        log.info("Henter krutkoder")

        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(fellesKodeverkUrl))
                .header(CORRELATION_HEADER, "flex-joark-mottak")
                .header(NAV_CONSUMER_ID, "flex-joark-mottak")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .GET()
                .build()
            val response = resilience.execute(request)

            if (response.statusCode() == 200) {
                return mapFKVStringToObject(response.body())
            } else {
                log.error("Klarte ikke hente Krutkoder fra Felles kodeverk")
                throw TemporarilyUnavailableException()
            }
        } catch (e: Exception) {
            log.error("Feil ved henting/parsing av KrutKoder: ${e.message}", e)
            throw ServiceUnavailableException()
        }
    }

    private fun mapFKVStringToObject(fellesKodeverkJson: String): FkvKrutkoder {
        try {
            return objectMapper.readValue(fellesKodeverkJson)
        } catch (e: Exception) {
            throw ServiceUnavailableException("Feil under dekoding av melding fra felles kodeverk: $fellesKodeverkJson")
        }
    }

    private fun excecute(req: HttpRequest): HttpResponse<String> {
        return client.send(req, HttpResponse.BodyHandlers.ofString())
    }
}
