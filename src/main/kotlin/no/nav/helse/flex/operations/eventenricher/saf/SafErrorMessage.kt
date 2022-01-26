package no.nav.helse.flex.operations.eventenricher.saf

data class SafErrorMessage(
    var timestamp: String? = null,
    var error: String? = null,
    var message: String? = null,
    var path: String? = null
) {
    var status = 0
}
