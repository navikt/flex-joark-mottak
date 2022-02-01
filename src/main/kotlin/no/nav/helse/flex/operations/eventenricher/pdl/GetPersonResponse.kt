package no.nav.helse.flex.operations.eventenricher.pdl

const val AKTORID = "AKTORID"
const val FOLKEREGISTERIDENT = "FOLKEREGISTERIDENT"

data class GetPersonResponse(
    val data: ResponseData,
    val errors: List<ResponseError>?
)

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?
)

data class ResponseData(
    val hentIdenter: HentIdenter? = null,
)

data class HentIdenter(
    val identer: List<Ident>
)

data class Ident(val gruppe: String, val ident: String) {
    val isAktoerId: Boolean
        get() = AKTORID == gruppe
    val isFNR: Boolean
        get() = FOLKEREGISTERIDENT == gruppe
}

data class ErrorLocation(
    val line: String?,
    val column: String?
)

data class ErrorExtension(
    val code: String?,
    val classification: String?
)
