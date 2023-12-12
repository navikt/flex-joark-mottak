package no.nav.helse.flex.felleskodeverk

class KodeverkBrevkoder(
    val hierarkinivaaer: List<String>,
    val noder: Map<String, TemaNode>
) {
    class TemaNode(
        val kode: String,
        val undernoder: Map<String, BrevkodeNode>
    ) {
        class BrevkodeNode(
            val kode: String,
            val termer: Map<String, String>
        ) {
            val behandlingstema get() = termer["nb"]?.split(";")?.get(0)
            val behandlingstype get() = termer["nb"]?.split(";")?.get(1)
        }
    }

    fun getBehandlingstema(journalpostTema: String, journalpostBrevkode: String?): String? {
        return noder[journalpostTema]?.undernoder?.get(journalpostBrevkode)?.behandlingstema
    }

    fun getBehandlingstype(journalpostTema: String, journalpostBrevkode: String?): String? {
        return noder[journalpostTema]?.undernoder?.get(journalpostBrevkode)?.behandlingstype
    }
}
