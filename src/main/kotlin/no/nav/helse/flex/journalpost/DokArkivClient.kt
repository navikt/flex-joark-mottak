package no.nav.helse.flex.journalpost

import no.nav.helse.flex.logger
import no.nav.helse.flex.refactor.ExternalServiceException
import no.nav.helse.flex.refactor.TemporarilyUnavailableException
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class DokArkivClient(
    @Value("\${DOKARKIV_URL}")
    private val dokarkivUrl: String,
    private val dokarkivRestTemplate: RestTemplate
) {
    private val log = logger()

    fun ferdigstillJournalpost(journalpostId: String) {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
            .path("/rest/journalpostapi/v1/journalpost/{id}/ferdigstill")
            .build(mapOf("id" to journalpostId))

        val response = dokarkivRestTemplate.exchange(
            uri,
            HttpMethod.PATCH,
            HttpEntity(FerdigstillJournalpostRequest().serialisertTilString(), headers),
            String::class.java
        )

        // TODO: Resttemplate kaster exception når status ikke er 2xx-ok
        if (response.statusCode.value() == 200) {
            // OK
        } else if (response.statusCode.value() == 503 || response.statusCode.value() == 404) {
            log.error("Feilkode mot Journalpost API ${response.statusCode.value()}")
            throw TemporarilyUnavailableException()
        } else {
            log.error("Klarte ikke ferdigstille journalpost $journalpostId, statuskode: ${response.statusCode.value()}")
            throw ExternalServiceException("Kunne ikke ferdigstille Journalpost", response.statusCode.value())
        }
    }

    fun updateJournalpost(journalpost: Journalpost) {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
            .path("/rest/journalpostapi/v1/journalpost/{id}")
            .build(mapOf("id" to journalpost.journalpostId))

        val response = dokarkivRestTemplate.exchange(
            uri,
            HttpMethod.PUT,
            HttpEntity(journalpost.serialisertTilString(), headers),
            String::class.java
        )

        // TODO: Resttemplate kaster exception når status ikke er 2xx-ok
        if (response.statusCode.value() == 200) {
            // OK
        } else if (response.statusCode.value() == 503 || response.statusCode.value() == 404) {
            throw TemporarilyUnavailableException()
        } else {
            log.error("Klarte ikke oppdatere journalpost ${journalpost.journalpostId}, statuskode: ${response.statusCode.value()}")
            throw ExternalServiceException("Kunne ikke oppdatere Journalpost", response.statusCode.value())
        }
    }
}

private const val CONTENT_TYPE_HEADER = "Content-Type"
private class FerdigstillJournalpostRequest {
    private val journalfoerendeEnhet = "9999"
}
