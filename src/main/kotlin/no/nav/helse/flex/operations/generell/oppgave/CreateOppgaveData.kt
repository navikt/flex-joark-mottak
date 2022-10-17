package no.nav.helse.flex.operations.generell.oppgave

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class CreateOppgaveData(
    var aktoerId: String? = null,
    var orgnr: String? = null,
    val journalpostId: String,
    var tema: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val oppgavetype: String,
    private val frist: Int,
    var tildeltEnhetsnr: String? = null,
    val beskrivelse: String? = null
) {
    val prioritet = "NORM"
    val aktivDato = LocalDate.now().toString()
    val fristFerdigstillelse = nextValidFrist(frist)

    fun removeJournalforendeEnhet() {
        tildeltEnhetsnr = null
    }

    fun removeOrgNr() {
        orgnr = null
    }

    private fun nextValidFrist(frist: Int): String {
        var oppgaveFrist = LocalDateTime.now().atZone(ZoneId.of("Europe/Oslo"))

        repeat(frist) {
            oppgaveFrist = oppgaveFrist.nesteUkedag()
        }

        if (oppgaveFrist.hour > 12) {
            oppgaveFrist = oppgaveFrist.nesteUkedag()
        }

        return oppgaveFrist.toLocalDate().toString()
    }

    private fun ZonedDateTime.nesteUkedag(): ZonedDateTime {
        return when (dayOfWeek) {
            DayOfWeek.FRIDAY -> plusDays(3)
            DayOfWeek.SATURDAY -> plusDays(2)
            else -> plusDays(1)
        }
    }
}
