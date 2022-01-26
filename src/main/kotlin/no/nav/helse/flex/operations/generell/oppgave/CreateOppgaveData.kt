package no.nav.helse.flex.operations.generell.oppgave

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class CreateOppgaveData(
    private val aktoerId: String? = null,
    val journalpostId: String,
    private val tema: String? = null,
    private val behandlingstema: String? = null,
    private val behandlingstype: String? = null,
    val oppgavetype: String,
    val frist: Int
) {
    private val prioritet = "NORM"
    private val aktivDato = LocalDate.now().toString()
    val fristFerdigstillelse = nextValidFrist(frist)
    private var tildeltEnhetsnr: String? = null

    fun setTildeltEnhetsnr(tildeltEnhetsnr: String?) {
        this.tildeltEnhetsnr = tildeltEnhetsnr
    }

    private fun nextValidFrist(frist: Int): String {
        var oppgaveFrist = LocalDateTime.now().atZone(ZoneId.of("Europe/Oslo"))

        var i = frist
        while (i > 0) {
            if (oppgaveFrist.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                i--
            }
            oppgaveFrist = oppgaveFrist.plusDays(1)
        }

        if (oppgaveFrist.hour > 12) {
            oppgaveFrist = oppgaveFrist.plusDays(1)
        }

        return oppgaveFrist.toLocalDate().toString()
    }
}
