package no.nav.helse.flex.operations.generell.felleskodeverk

data class Beskrivelse(
    val term: String,
    val tekst: String
) {
    fun init(): TemaSkjemaData {
        val tittelBrevkodeBehandlingstemaBehandlingstype = term
            .split(";".toRegex(), -1)

        return TemaSkjemaData(
            tittelBrevkodeBehandlingstemaBehandlingstype[0],
            tittelBrevkodeBehandlingstemaBehandlingstype[1],
            tittelBrevkodeBehandlingstemaBehandlingstype[2],
            tittelBrevkodeBehandlingstemaBehandlingstype[3]
        )
    }
}
