package no.nav.helse.flex.operations.eventenricher.journalpost

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

@JsonIgnoreProperties(ignoreUnknown = true)
class Journalpost {
    var bruker: Bruker? = null
    var tittel: String? = null
    lateinit var journalpostId: String
    lateinit var journalstatus: String
    lateinit var dokumenter: List<Dokument>
    var journalforendeEnhet: String? = null
    var relevanteDatoer: List<RelevanteDatoer>? = null
    var sak: Sak? = null
    lateinit var tema: String
    var avsenderMottaker: AvsenderMottaker? = null
    var behandlingstema: String? = null
    var behandlingstype: String? = null
    val brevkode: String?
        get() = if (dokumenter.isNotEmpty()) dokumenter[0].brevkode else ""

    fun invalidJournalpostStatus(): Boolean {
        return !(JOURNALSTATUS_MOTTATT == journalstatus || JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT == journalstatus)
    }

    fun updateWithGenerellSak() {
        sak = Sak()
    }

    fun toJson(): String {
        val builder = StringBuilder()
        return builder.append("{")
            .append("\"tittel\":\"").append(tittel).append("\",")
            .append("\"tema\":\"").append(tema).append("\",")
            .append(avsenderMottaker!!.toJson()).append(",")
            .append(sak!!.toJson()).append(",")
            .append(bruker!!.toJson())
            .append("}")
            .toString()
    }

    override fun toString(): String {
        return ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("id", journalpostId)
            .append("tema", tema)
            .append("skjema", brevkode)
            .append("tittel", tittel)
            .append("journalforendeEnhet", journalforendeEnhet)
            .append("journalstatus", journalstatus)
            .append("behandlingstema", behandlingstema)
            .toString()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Bruker(val id: String, private val type: String) {
        val isAktoerId: Boolean
            get() = AKTOERID == type
        val isFNR: Boolean
            get() = PERSONBRUKER == type
        val isORGNR: Boolean
            get() = ORGANISASJON == type

        fun toJson(): String {
            return """"bruker": {"id":"$id","idType":"$type"}"""
        }

        companion object {
            private const val AKTOERID = "AKTOERID"
            private const val PERSONBRUKER = "FNR"
            private const val ORGANISASJON = "ORGNR"
        }
    }

    class RelevanteDatoer(private val dato: String, private val datotype: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Sak {
        private val sakstype: String = SAKSTYPE_GENERELL

        fun toJson(): String {
            return """"sak": {"sakstype":"$sakstype"}"""
        }

        companion object {
            private const val SAKSTYPE_GENERELL = "GENERELL_SAK"
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class AvsenderMottaker internal constructor(var id: String?, private val type: String) {
        fun toJson(): String {
            return """"avsenderMottaker": {"id":"$id","idType":"$type"}"""
        }
    }

    companion object {
        private const val JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT = "M"
        private const val JOURNALSTATUS_MOTTATT = "MOTTATT"
    }
}
