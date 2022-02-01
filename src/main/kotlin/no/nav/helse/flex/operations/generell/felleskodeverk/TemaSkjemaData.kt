package no.nav.helse.flex.operations.generell.felleskodeverk

data class TemaSkjemaData(
    val tittel: String,
    val brevkode: String,
    val behandlingstema: String?,
    val behandlingstype: String?
)
