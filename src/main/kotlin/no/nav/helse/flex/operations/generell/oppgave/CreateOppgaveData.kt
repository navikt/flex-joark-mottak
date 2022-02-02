package no.nav.helse.flex.operations.generell.oppgave

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class CreateOppgaveData(
    val aktoerId: String? = null,
    val journalpostId: String,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String,
    private val frist: Int
) {
    val prioritet = "NORM"
    val aktivDato = LocalDate.now().toString()
    val fristFerdigstillelse = nextValidFrist(frist)
    var tildeltEnhetsnr: String? = null

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
