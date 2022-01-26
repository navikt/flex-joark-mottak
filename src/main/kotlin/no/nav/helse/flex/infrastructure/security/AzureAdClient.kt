package no.nav.helse.flex.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.Environment.azureAppClientSecret
import no.nav.helse.flex.Environment.azureAppURL
import no.nav.helse.flex.Environment.azureClientId
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import java.util.stream.Collectors

class AzureAdClient(private val clientId: String) {
    private val log = LoggerFactory.getLogger(AzureAdClient::class.java)

    private var token: String
    private val grantType = "client_credentials"
    private val discoveryUrl: String

    init {
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder().uri(URI.create(azureAppURL))
                .GET()
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            try {
                val result: HashMap<String, Any> = ObjectMapper().readValue(response.body())
                discoveryUrl = result["token_endpoint"].toString()
            } catch (e: IOException) {
                throw IllegalStateException("Klarte ikke deserialisere respons fra AzureAd", e)
            }
        } catch (e: Exception) {
            log.error("Exception under static INIT av AAD client")
            log.error(e.message)
            throw IllegalStateException(e)
        }
        token = tokenFromAzureAd
    }

    fun getToken(): String {
        if (isExpired(token)) {
            token = tokenFromAzureAd
        }
        return "Bearer $token"
    }

    private val tokenFromAzureAd: String
        get() = try {
            val parameters: MutableMap<String, String> = HashMap()
            parameters["scope"] = "api://$clientId/.default"
            parameters["client_id"] = azureClientId
            parameters["client_secret"] = azureAppClientSecret
            parameters["grant_type"] = grantType
            val form = parameters.keys.stream()
                .map { key: String -> key + "=" + URLEncoder.encode(parameters[key], StandardCharsets.UTF_8) }
                .collect(Collectors.joining("&"))
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder().uri(URI.create(discoveryUrl))
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val azureAdResponse: AzureAdResponse

            try {
                azureAdResponse = ObjectMapper().readValue(response.body())
                azureAdResponse.access_token
            } catch (e: IOException) {
                log.error(e.message)
                throw IllegalStateException("Klarte ikke deserialisere respons fra AzureAd", e)
            }
        } catch (e: Exception) {
            log.error(e.message)
            throw IllegalArgumentException(e)
        }

    private fun isExpired(AADToken: String): Boolean {
        val mapper = ObjectMapper()
        try {
            val tokenBody = String(Base64.getDecoder().decode(StringUtils.substringBetween(AADToken, ".")))
            val node = mapper.readTree(tokenBody)
            val now = Instant.now()
            val expiry = Instant.ofEpochSecond(node["exp"].longValue()).minusSeconds(300)
            if (now.isAfter(expiry)) {
                log.debug("AzureAd token expired. {} is after {}", now, expiry)
                return true
            }
        } catch (e: IOException) {
            log.error("Klarte ikke parse token fra AzureAd")
            throw IllegalStateException("Klarte ikke parse token fra AzureAd", e)
        }
        return false
    }
}
