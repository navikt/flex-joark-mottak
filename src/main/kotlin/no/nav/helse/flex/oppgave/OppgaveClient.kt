package no.nav.helse.flex.oppgave

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.*

private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val PARAM_STATUSKATEGORI_AAPEN = "AAPEN"
private const val PARAM_OPPGAVETYPE_JFR = "JFR"
private const val PARAM_OPPGAVETYPE_FDR = "FDR"

@Component
class OppgaveClient(
    @param:Value("\${OPPGAVE_URL}")
    private val oppgaveUrl: String,
    private val oppgaveRestTemplate: RestTemplate,
) {
    private val log = logger()

    fun opprettOppgave(requestData: OppgaveRequest): Oppgave {
        val response =
            runCatching {
                opprettOppgaveKall(requestData)
            }.recover { httpException ->
                if (httpException is HttpClientErrorException && httpException.statusCode.value() == 400) {
                    val oppgaveErrorResponse =
                        try {
                            objectMapper.readValue<OppgaveErrorResponse>(httpException.responseBodyAsByteArray)
                        } catch (_: Exception) {
                            throw httpException
                        }

                    if (oppgaveErrorResponse.isErrorInvalidEnhet()) {
                        log.error(
                            "Kunne ikke opprette oppgave på grunn av ugyldig enhet på journalpost: ${requestData.journalpostId}.",
                            oppgaveErrorResponse.feilmelding,
                        )
                        requestData.removeJournalforendeEnhet()
                        return@recover opprettOppgaveKall(requestData)
                    }

                    if (oppgaveErrorResponse.isErrorInvalidOrgNr()) {
                        log.error(
                            "Kunne ikke opprette oppgave på grunn av ugyldig OrgNr på journalpost: ${requestData.journalpostId}.",
                            oppgaveErrorResponse.feilmelding,
                        )
                        requestData.removeOrgNr()
                        return@recover opprettOppgaveKall(requestData)
                    }
                }
                throw httpException
            }.getOrThrow()

        val oppgave = objectMapper.readValue<Oppgave>(response.body!!)
        log.info(
            "Opprettet oppgave med type: ${oppgave.oppgavetype} og id: ${oppgave.id} på enhet: ${oppgave.tildeltEnhetsnr} for " +
                "journalpost: ${requestData.journalpostId}.",
        )

        return oppgave
    }

    private fun opprettOppgaveKall(requestData: OppgaveRequest): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        return oppgaveRestTemplate.exchange(
            "$oppgaveUrl/api/v1/oppgaver",
            HttpMethod.POST,
            HttpEntity(requestData.serialisertTilString(), headers),
            String::class.java,
        )
    }

    fun finnesOppgaveForJournalpost(journalpostId: String): Boolean {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE

        val uri =
            UriComponentsBuilder
                .fromUriString(oppgaveUrl)
                .path("/api/v1/oppgaver")
                .queryParam("statuskategori", PARAM_STATUSKATEGORI_AAPEN)
                .queryParam("oppgavetype", PARAM_OPPGAVETYPE_JFR)
                .queryParam("oppgavetype", PARAM_OPPGAVETYPE_FDR)
                .queryParam("journalpostId", "{id}")
                .encode()
                .toUriString()

        val response =
            oppgaveRestTemplate.exchange(
                uri,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                String::class.java,
                mapOf("id" to journalpostId),
            )

        return objectMapper.readValue<OppgaveSearchResponse>(response.body!!).harTilknyttetOppgave()
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
    var versjon: Int = 0,
)

data class OppgaveErrorResponse(
    val uuid: String,
    val feilmelding: String,
) {
    fun isErrorInvalidEnhet(): Boolean =
        (
            feilmelding.contains("NAVEnheten '") &&
                feilmelding.contains("' er av typen oppgavebehandler") ||
                feilmelding.contains("NAVEnheten '") &&
                feilmelding.contains("' har status: 'Nedlagt'") ||
                feilmelding.contains("Enheten med nummeret '") &&
                feilmelding.contains("' eksisterer ikke")
        )

    fun isErrorInvalidOrgNr(): Boolean = (feilmelding.contains("Organisasjonsnummer er ugyldig"))
}

data class OppgaveSearchResponse(
    val antallTreffTotalt: Int = 0,
    val oppgaver: List<Oppgave>? = null,
) {
    fun harTilknyttetOppgave(): Boolean = (antallTreffTotalt > 0 && !oppgaver.isNullOrEmpty() && oppgaver[0].id != null)
}

data class OppgaveRequest(
    var aktoerId: String? = null,
    var orgnr: String? = null,
    val journalpostId: String,
    var tema: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String,
    var tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null,
    private val frist: Int,
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

    private fun ZonedDateTime.nesteUkedag(): ZonedDateTime =
        when (dayOfWeek) {
            DayOfWeek.FRIDAY -> plusDays(3)
            DayOfWeek.SATURDAY -> plusDays(2)
            else -> plusDays(1)
        }
}
