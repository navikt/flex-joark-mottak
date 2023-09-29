package no.nav.helse.flex.journalpost

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
    fun ferdigstillJournalpost(journalpostId: String) {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
            .path("/rest/journalpostapi/v1/journalpost/{id}/ferdigstill")
            .build(mapOf("id" to journalpostId))

        dokarkivRestTemplate.exchange(
            uri,
            HttpMethod.PATCH,
            HttpEntity(FerdigstillJournalpostRequest().serialisertTilString(), headers),
            String::class.java
        )
    }

    fun updateJournalpost(journalpost: Journalpost) {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(dokarkivUrl)
            .path("/rest/journalpostapi/v1/journalpost/{id}")
            .build(mapOf("id" to journalpost.journalpostId))

        dokarkivRestTemplate.exchange(
            uri,
            HttpMethod.PUT,
            HttpEntity(journalpost.serialisertTilString(), headers),
            String::class.java
        )
    }
}

private const val CONTENT_TYPE_HEADER = "Content-Type"
private class FerdigstillJournalpostRequest {
    private val journalfoerendeEnhet = "9999"
}
