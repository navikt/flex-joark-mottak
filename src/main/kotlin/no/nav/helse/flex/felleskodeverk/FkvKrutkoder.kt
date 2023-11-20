package no.nav.helse.flex.felleskodeverk

import org.slf4j.LoggerFactory
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.StringBuilder
import java.util.HashMap

data class FkvKrutkoder(
    val betydninger: Map<String, List<Betydning>> = emptyMap()
) {
    private val log = LoggerFactory.getLogger(FkvKrutkoder::class.java)
    private val skjemaMangler = ""
    private val temaSkjemaDataMap: MutableMap<String, TemaSkjemaData> = HashMap()

    init {
        for ((key, betydning) in betydninger) {
            try {
                temaSkjemaDataMap[key] = betydning.first().init()
            } catch (ie: IndexOutOfBoundsException) {
                log.error(
                    "Feil ved dekoding av felles kodeverk, feil format i fkv - betydning ($key) - liste($betydning)"
                )
            }
        }
    }

    internal fun getTemaSkjema(temaSkjema: String): TemaSkjemaData? {
        if (temaSkjemaDataMap.containsKey(temaSkjema.trim { it <= ' ' })) {
            return temaSkjemaDataMap[temaSkjema.trim { it <= ' ' }]
        } else {
            throw IllegalArgumentException("Skjema/Tema $temaSkjema finnes ikke i kodeverket")
        }
    }

    private fun lagTemaSkjemaNokkel(tema: String, skjema: String): String {
        return skjema.trim { it <= ' ' } + ":" + tema.trim { it <= ' ' }
    }

    fun getBehandlingstype(tema: String?, skjema: String?): String? {
        return if (tema.isNullOrBlank() || skjema.isNullOrBlank()) {
            skjemaMangler
        } else {
            getTemaSkjema(lagTemaSkjemaNokkel(tema, skjema))?.behandlingstype
        }
    }

    fun getBehandlingstema(tema: String?, skjema: String?): String? {
        return if (tema.isNullOrBlank() || skjema.isNullOrBlank()) {
            skjemaMangler
        } else {
            getTemaSkjema(
                lagTemaSkjemaNokkel(tema, skjema)
            )?.behandlingstema
        }
    }

    override fun toString(): String {
        val toString = StringBuilder("TemaSkjemaRuting Felles kodeverk:\n")
        for (key in temaSkjemaDataMap.keys) {
            toString.append(key).append(",\n")
        }
        return toString.toString()
    }
}

data class TemaSkjemaData(
    val tittel: String,
    val brevkode: String,
    val behandlingstema: String,
    val behandlingstype: String
)

data class Betydning(
    var gyldigFra: String,
    var gyldigTil: String,
    var beskrivelser: Map<String, Beskrivelse>
) {
    fun init(): TemaSkjemaData {
        return beskrivelser["nb"]!!.init()
    }
}

data class Beskrivelse(
    val term: String,
    val tekst: String
) {
    fun init(): TemaSkjemaData {
        val tittelBrevkodeBehandlingstemaBehandlingstype = term
            .split(";".toRegex())

        return TemaSkjemaData(
            tittelBrevkodeBehandlingstemaBehandlingstype[0],
            tittelBrevkodeBehandlingstemaBehandlingstype[1],
            tittelBrevkodeBehandlingstemaBehandlingstype[2],
            tittelBrevkodeBehandlingstemaBehandlingstype[3]
        )
    }
}
