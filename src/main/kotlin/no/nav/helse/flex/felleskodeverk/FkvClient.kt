package no.nav.helse.flex.felleskodeverk

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import javax.naming.ServiceUnavailableException

@Component
class FkvClient(
    @Value("\${FKV_URL}")
    private val fkvUrl: String,
    private val plainRestTemplate: RestTemplate
) {
    private val log = logger()

    @Cacheable("krutkoder")
    fun hentKrutkoder(): FkvKrutkoder {
        log.info("Henter og cacher [krutkoder]")

        val headers = HttpHeaders()
        headers[CORRELATION_HEADER] = "flex-joark-mottak"
        headers[NAV_CONSUMER_ID] = "flex-joark-mottak"

        val uri = UriComponentsBuilder.fromHttpUrl(fkvUrl)
            .path("/api/v1/kodeverk/Krutkoder/koder/betydninger")
            .queryParam("spraak", "nb")
            .build()
            .toUri()

        val response = plainRestTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            String::class.java
        )

        return objectMapper.readValue<FkvKrutkoder>(response.body!!)
    }
}

private val CORRELATION_HEADER = "Nav-Call-Id"
private val NAV_CONSUMER_ID = "Nav-Consumer-Id"
