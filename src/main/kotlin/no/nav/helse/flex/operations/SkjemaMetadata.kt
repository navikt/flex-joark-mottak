package no.nav.helse.flex.operations

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import no.nav.helse.flex.Environment.getSkjemaerJson
import java.util.*

class SkjemaMetadata {
    private val temaMap: HashMap<String, TemaKodeverk>

    init {
        val gson = Gson()
        val jsonString = getSkjemaerJson()
        temaMap = gson.fromJson(jsonString, object : TypeToken<HashMap<String?, TemaKodeverk?>?>() {}.type)
    }

    fun inAutoList(tema: String, skjema: String?): Boolean {
        return temaMap[tema]?.hasSkjema(skjema) ?: false
    }

    fun getFrist(tema: String, skjema: String?): Int {
        return temaMap[tema]!!.getFristFromSkjema(skjema)
    }

    fun getOppgavetype(tema: String, skjema: String?): String {
        return temaMap[tema]!!.getOppgavetypeFromSkjema(skjema)
    }

    fun isIgnoreskjema(tema: String, skjema: String?): Boolean {
        return temaMap[tema]!!.isIgnoreSkjema(skjema)
    }

    private data class TemaKodeverk(
        private val skjemaer: HashMap<String?, SkjemaKodeverk>? = null,
        private val ignoreSkjema: List<String?>? = null
    ) {

        fun hasSkjema(skjema: String?): Boolean {
            return if (skjemaer == null || skjemaer.isEmpty()) {
                false
            } else skjemaer.containsKey(Objects.requireNonNullElse(skjema, "null"))
        }

        fun isIgnoreSkjema(skjema: String?): Boolean {
            return ignoreSkjema?.contains(skjema) ?: false
        }

        fun getFristFromSkjema(skjema: String?): Int {
            return skjemaer!![skjema]!!.frist
        }

        fun getOppgavetypeFromSkjema(skjema: String?): String {
            return skjemaer!![skjema]!!.oppgavetype
        }

        private inner class SkjemaKodeverk(val frist: Int, val oppgavetype: String)
    }
}
