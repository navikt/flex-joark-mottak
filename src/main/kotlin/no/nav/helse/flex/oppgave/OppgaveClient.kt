package no.nav.helse.flex.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
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
import java.time.*

private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val PARAM_STATUSKATEGORI_AAPEN = "AAPEN"
private const val PARAM_OPPGAVETYPE_JFR = "JFR"
private const val PARAM_OPPGAVETYPE_FDR = "FDR"

@Component
class OppgaveClient(
    @Value("\${OPPGAVE_URL}")
    private val oppgaveUrl: String,
    private val oppgaveRestTemplate: RestTemplate
) {
    private val log = logger()

    fun createOppgave(requestData: OppgaveRequest): Oppgave {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val response = oppgaveRestTemplate.exchange(
            "$oppgaveUrl/api/v1/oppgaver",
            HttpMethod.POST,
            HttpEntity(requestData.serialisertTilString(), headers),
            String::class.java
        )

        // TODO: Resttemplate kaster exception når status ikke er 2xx-ok
        if (response.statusCode.value() == 201) {
            val oppgave = objectMapper.readValue<Oppgave>(response.body!!)
            log.info("Opprettet ${oppgave.oppgavetype}-oppgave: ${oppgave.id} på enhet ${oppgave.tildeltEnhetsnr} for journalpost: ${requestData.journalpostId}")
            return oppgave
        } else if (response.statusCode.value() == 404) {
            log.error("Klarte ikke opprette oppgave på journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode.value()}")
            throw TemporarilyUnavailableException()
        } else if (response.statusCode.is5xxServerError) {
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke opprette oppgave på journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode.value()}. $errorText")
            throw TemporarilyUnavailableException()
        } else {
            val errorText = hentFeilmelding(response)
            log.error("Klarte ikke opprette oppgave for journalpost ${requestData.journalpostId}, statuskode: ${response.statusCode}. $errorText")
            throw ExternalServiceException(errorText, response.statusCode.value())
        }
    }

    fun finnesOppgaveForJournalpost(journalpostId: String): Boolean {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri = UriComponentsBuilder.fromHttpUrl(oppgaveUrl)
            .path("/api/v1/oppgaver")
            .queryParam("statuskategori", PARAM_STATUSKATEGORI_AAPEN)
            .queryParam("oppgavetype", PARAM_OPPGAVETYPE_JFR)
            .queryParam("oppgavetype", PARAM_OPPGAVETYPE_FDR)
            .queryParam("journalpostId", "{id}")
            .encode()
            .toUriString()

        val response = oppgaveRestTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            String::class.java,
            mapOf("id" to journalpostId)
        )

        return objectMapper.readValue<OppgaveSearchResponse>(response.body!!).harTilknyttetOppgave()
    }

    private fun hentFeilmelding(response: HttpEntity<String>): String {
        val oppgaveErrorResponse: OppgaveErrorResponse = objectMapper.readValue(response.body!!)
        return oppgaveErrorResponse.feilmelding
    }
}

data class Oppgave(
    var id: String? = null,
    var beskrivelse: String? = null,
    var saksreferanse: String? = null,
    var status: String? = null,
    var oppgavetype: String? = null,
    var tema: String? = null,
    var tildeltEnhetsnr: String? = null,
    var versjon: Int = 0
)

data class OppgaveErrorResponse(
    val uuid: String,
    val feilmelding: String
)

data class OppgaveSearchResponse(
    val antallTreffTotalt: Int = 0,
    val oppgaver: List<Oppgave>? = null
) {
    fun harTilknyttetOppgave(): Boolean {
        return (antallTreffTotalt > 0 && !oppgaver.isNullOrEmpty() && oppgaver[0].id != null)
    }
}

data class OppgaveRequest(
    var aktoerId: String? = null,
    var orgnr: String? = null,
    val journalpostId: String,
    var tema: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String,
    val frist: Int,
    var tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null
) {
    val prioritet = "NORM"
    val aktivDato = LocalDate.now().toString()
    val fristFerdigstillelse = nextValidFrist(frist)

    fun removeJournalforendeEnhet() {
        tildeltEnhetsnr = null
    }

    fun removeOrgNr() {
        orgnr = null
    }

    private fun nextValidFrist(frist: Int): String {
        var oppgaveFrist = LocalDateTime.now().atZone(ZoneId.of("Europe/Oslo"))

        repeat(frist) {
            oppgaveFrist = oppgaveFrist.nesteUkedag()
        }

        if (oppgaveFrist.hour > 12) {
            oppgaveFrist = oppgaveFrist.nesteUkedag()
        }

        return oppgaveFrist.toLocalDate().toString()
    }

    private fun ZonedDateTime.nesteUkedag(): ZonedDateTime {
        return when (dayOfWeek) {
            DayOfWeek.FRIDAY -> plusDays(3)
            DayOfWeek.SATURDAY -> plusDays(2)
            else -> plusDays(1)
        }
    }
}
