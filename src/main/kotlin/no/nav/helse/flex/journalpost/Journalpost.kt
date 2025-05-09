package no.nav.helse.flex.journalpost

data class Journalpost(
    val bruker: Bruker? = null,
    val tittel: String? = null,
    val journalpostId: String,
    val journalstatus: String,
    val dokumenter: List<Dokument>,
    val journalforendeEnhet: String? = null,
    val relevanteDatoer: List<RelevanteDatoer>? = null,
    val sak: Sak? = null,
    val tema: String,
    val avsenderMottaker: AvsenderMottaker? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
) {
    val brevkode: String?
        get() = if (dokumenter.isNotEmpty()) dokumenter[0].brevkode else null

    fun invalidJournalpostStatus(): Boolean =
        !(JOURNALSTATUS_MOTTATT == journalstatus || JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT == journalstatus)

    override fun toString(): String =
        "[id=$journalpostId, tema=$tema, skjema=$brevkode, tittel=$tittel, " +
            "journalforendeEnhet=$journalforendeEnhet, journalstatus=$journalstatus, " +
            "behandlingstema=$behandlingstema, behandlingstype=$behandlingstype]"

    class Bruker(
        val id: String,
        // Denne er tilstede når vi henter i fra Saf.
        private val type: String,
    ) {
        // Denne bruker vi når vi oppdaterer journalpost i DokArkiv.
        val idType: String = type
        val isAktoerId
            get() = AKTOERID == type
        val isFNR
            get() = PERSONBRUKER == type
        val isORGNR
            get() = ORGANISASJON == type

        companion object {
            private const val AKTOERID = "AKTOERID"
            private const val PERSONBRUKER = "FNR"
            private const val ORGANISASJON = "ORGNR"
        }
    }

    class RelevanteDatoer(
        val dato: String,
        val datotype: String,
    )

    class Sak(
        val sakstype: String,
    )

    class AvsenderMottaker(
        var id: String?,
        // Denne er satt når vi henter journalpost og er ikke samme som vi bruker når vi oppdaterer journalpost.
        private val type: String,
    ) {
        // Brukes i oppdater Journalpost request.
        val idType: String = type
    }

    class Dokument(
        val brevkode: String?,
        val tittel: String? = null,
        val dokumentInfoId: String,
    )

    companion object {
        private const val JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT = "M"
        private const val JOURNALSTATUS_MOTTATT = "MOTTATT"
    }
}
