package no.nav.helse.flex.operations.generell.felleskodeverk

data class Betydning(
    var gyldigFra: String,
    var gyldigTil: String,
    var beskrivelser: Map<String, Beskrivelse>
) {
    fun init(): TemaSkjemaData {
        return beskrivelser["nb"]!!.init()
    }
}
