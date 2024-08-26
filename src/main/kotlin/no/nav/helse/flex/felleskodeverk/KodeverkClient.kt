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

@Component
class KodeverkClient(
    @Value("\${KODEVERK_URL}")
    private val kodeverkUrl: String,
    private val kodeverkRestTemplate: RestTemplate,
) {
    private val log = logger()

    @Cacheable("krutkoder")
    fun hentKrutkoder(): KodeverkBrevkoder {
        val headers = HttpHeaders()
        headers[CORRELATION_HEADER] = "flex-joark-mottak"
        headers[NAV_CONSUMER_ID] = "flex-joark-mottak"

        val uri =
            UriComponentsBuilder.fromHttpUrl(kodeverkUrl)
                .path("/api/v1/hierarki/TemaSkjemaGjelder/noder")
                .queryParam("spraak", "nb")
                .encode()
                .toUriString()

        val response =
            kodeverkRestTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java,
            )

        log.info("Hentet og cachet brevkoder fra Felles Kodeverk.")
        return objectMapper.readValue<KodeverkBrevkoder>(response.body!!)
    }
}

private const val CORRELATION_HEADER = "Nav-Call-Id"
private const val NAV_CONSUMER_ID = "Nav-Consumer-Id"
