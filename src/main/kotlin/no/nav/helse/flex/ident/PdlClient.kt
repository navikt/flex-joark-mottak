package no.nav.helse.flex.ident

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.graphql.GraphQLRequest
import no.nav.helse.flex.graphql.GraphQLResponse
import no.nav.helse.flex.journalpost.FinnerIkkePersonException
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.lang.Exception

@Component
class PdlClient(
    @Value("\${PDL_URL}")
    private val pdlApiUrl: String,
    private val pdlRestTemplate: RestTemplate,
) {
    private val log = logger()

    @Retryable(exclude = [FinnerIkkePersonException::class])
    fun hentIdenterForJournalpost(journalpost: Journalpost): List<PdlIdent> {
        val headers = HttpHeaders()
        headers[CONTENT_TYPE_HEADER] = MediaType.APPLICATION_JSON_VALUE
        headers[TEMA] = TEMA_SYK

        val responseEntity: ResponseEntity<String> =
            pdlRestTemplate.exchange(
                "$pdlApiUrl/graphql",
                HttpMethod.POST,
                HttpEntity(
                    GraphQLRequest(HENT_IDENTER, mapOf(IDENT to journalpost.bruker!!.id)).serialisertTilString(),
                    headers,
                ),
                String::class.java,
            )

        if (!responseEntity.statusCode.is2xxSuccessful) {
            throw Exception(
                "Kall til PDL for journalpost: ${journalpost.journalpostId} feilet med " +
                    "statuskode: ${responseEntity.statusCode.value()}.",
            )
        }

        if (responseEntity.body == null) {
            throw Exception(
                "Mangler body i response fra PDL for journalpost: ${journalpost.journalpostId} med " +
                    "statuskode: ${responseEntity.statusCode.value()}.",
            )
        }

        val parsedResponse = responseEntity.body!!.let { objectMapper.readValue<GraphQLResponse<HentIdenterData>>(it) }

        parsedResponse.errors?.forEach {
            log.error("Feilet ved parsing av fra PDL for journalpost: ${journalpost.journalpostId}.", it.serialisertTilString())
        }

        return parsedResponse.data.hentIdenter?.identer ?: throw FinnerIkkePersonException()
    }
}

data class HentIdenterData(
    val hentIdenter: HentIdenter? = null,
)

data class HentIdenter(
    val identer: List<PdlIdent>,
)

data class PdlIdent(
    val gruppe: String,
    val ident: String,
)

private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val TEMA = "Tema"
private const val TEMA_SYK = "SYK"
private const val IDENT = "ident"
const val AKTORID = "AKTORID"
const val FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT"

private const val HENT_IDENTER = """
query(${"$"}ident: ID!){
  hentIdenter(ident: ${"$"}ident, historikk: false) {
    identer {
      ident,
      gruppe
    }
  }
}
"""
