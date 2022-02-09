package no.nav.helse.flex.operations.generell.oppgave

data class OppgaveSearchResponse(
    val antallTreffTotalt: Int = 0,
    val oppgaver: List<Oppgave>? = null
) {
    fun harTilknyttetOppgave(): Boolean {
        return (antallTreffTotalt > 0 && !oppgaver.isNullOrEmpty() && oppgaver[0].id != null)
    }
}
