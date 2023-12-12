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
        val brevkodeNode = noder[journalpostTema]?.undernoder?.get(journalpostBrevkode) ?: return null
        return brevkodeNode.behandlingstema.takeUnless { it.isNullOrBlank() }
    }

    fun getBehandlingstype(journalpostTema: String, journalpostBrevkode: String?): String? {
        val brevkodeNode = noder[journalpostTema]?.undernoder?.get(journalpostBrevkode) ?: return null
        return brevkodeNode.behandlingstype.takeUnless { it.isNullOrBlank() }
    }
}
