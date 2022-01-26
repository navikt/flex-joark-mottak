package no.nav.helse.flex.operations.eventenricher.pdl

data class Ident(
    val iD: String,
    private val historisk: Boolean,
    private val gruppe: String
) {
    val isAktoerId: Boolean
        get() = AKTOERID == gruppe
    val isFNR: Boolean
        get() = FNR == gruppe

    companion object {
        private const val AKTOERID = "AKTORID"
        private const val FNR = "FOLKEREGISTERIDENT"
    }
}
