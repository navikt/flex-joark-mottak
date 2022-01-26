package no.nav.helse.flex.operations.generell.felleskodeverk

import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.StringBuilder
import java.util.*

class FkvKrutkoder {
    private val log = LoggerFactory.getLogger(FkvKrutkoder::class.java)
    private val skjemaMangler = ""
    private var betydninger: Map<String, List<Betydning>>? = null
    private val temaSkjemaDataMap: MutableMap<String, TemaSkjemaData> = HashMap()

    fun init() {
        val betydningNokkler = betydninger!!.keys
        for (key in betydningNokkler) {
            val betydningListe = betydninger!![key]!!
            try {
                val betydning = betydningListe[0]
                temaSkjemaDataMap[key] = betydning.init()
            } catch (ie: IndexOutOfBoundsException) {
                log.error(
                    "Feil ved dekoding av felles kodeverk, feil format i fkv - betydning ($key) - liste($betydningListe)"
                )
            }
        }
    }

    internal fun getTemaSkjema(temaSkjema: String): TemaSkjemaData? {
        return if (temaSkjemaDataMap.containsKey(temaSkjema.trim { it <= ' ' })) {
            temaSkjemaDataMap[temaSkjema.trim { it <= ' ' }]
        } else Optional.ofNullable(temaSkjemaDataMap[temaSkjema.trim { it <= ' ' }])
            .orElseThrow { IllegalArgumentException("Skjema/Tema $temaSkjema finnes ikke i kodeverket") }
    }

    private fun lagTemaSkjemaNokkel(tema: String, skjema: String): String {
        return skjema.trim { it <= ' ' } + ":" + tema.trim { it <= ' ' }
    }

    fun getBehandlingstype(tema: String?, skjema: String?): String {
        return if (tema == null || tema.isBlank() || skjema == null || skjema.isBlank()) {
            skjemaMangler
        } else getTemaSkjema(lagTemaSkjemaNokkel(tema, skjema))!!.behandlingstype
    }

    fun getBehandlingstema(tema: String?, skjema: String?): String {
        return if (tema.isNullOrBlank() || skjema.isNullOrBlank()) {
            skjemaMangler
        } else getTemaSkjema(
            lagTemaSkjemaNokkel(tema, skjema)
        )!!.behandlingstema
    }

    override fun toString(): String {
        val toString = StringBuilder("TemaSkjemaRuting Felles kodeverk:\n")
        for (key in temaSkjemaDataMap.keys) {
            toString.append(key).append(",\n")
        }
        return toString.toString()
    }
}
