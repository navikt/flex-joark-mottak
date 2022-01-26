package no.nav.helse.flex.operations.generell.felleskodeverk

data class Betydning(
    var gyldigFra: String? = null,
    var gyldigTil: String? = null,
    var beskrivelser: Map<String, Beskrivelse>? = null
) {
    fun init(): TemaSkjemaData {
        return beskrivelser!!["nb"]!!.init()
    }
}
