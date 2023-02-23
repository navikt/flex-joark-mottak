package no.nav.helse.flex.operations

import java.util.*

object SkjemaMetadata {
    private val temaMap: HashMap<String, TemaKodeverk> = hashMapOf(
        "SYK" to TemaKodeverk(
            skjemaer = hashMapOf(
                "NAV 08-07.04D" to SkjemaKodeverk("SOK", 3),
                "NAV 08-07.04 D" to SkjemaKodeverk("SOK", 3)
            ),
            ignoreSkjema = listOf(
                "4936",
                "krav_om_fritak_fra_agp_gravid",
                "krav_om_fritak_fra_agp_kronisk",
                "soeknad_om_fritak_fra_agp_kronisk",
                "refusjonskrav_arbeidsgiverperiode_korona",
                "soeknad_om_fritak_fra_agp_gravid",
                "refusjonskrav_utestengt_arbeider_korona",
                "annuller_refusjonskrav_utestengt_arbeider_korona"
            )
        )
    )

    fun inAutoList(tema: String, skjema: String?): Boolean {
        return temaMap[tema]?.skjemaer?.containsKey(skjema) ?: false
    }

    fun getFrist(tema: String, skjema: String?): Int {
        return temaMap[tema]!!.skjemaer[skjema]!!.frist
    }

    fun getOppgavetype(tema: String, skjema: String?): String {
        return temaMap[tema]!!.skjemaer[skjema]!!.oppgavetype
    }

    fun isIgnoreskjema(tema: String, skjema: String?): Boolean {
        return temaMap[tema]?.ignoreSkjema?.contains(skjema) ?: false
    }
}

private class TemaKodeverk(
    val skjemaer: HashMap<String, SkjemaKodeverk>,
    val ignoreSkjema: List<String?>
)

private class SkjemaKodeverk(
    val oppgavetype: String,
    val frist: Int
)
