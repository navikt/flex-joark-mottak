package no.nav.helse.flex.operations.generell.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Oppgave(
    var id: String? = null,
    var beskrivelse: String? = null,
    var saksreferanse: String? = null,
    var status: String? = null,
    var oppgavetype: String? = null,
    var tema: String? = null,
    var tildeltEnhetsnr: String? = null
) {
    var versjon = 0
}
