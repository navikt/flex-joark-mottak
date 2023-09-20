package no.nav.helse.flex.ident

import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.logger
import no.nav.helse.flex.oppgave.OppgaveClient
import no.nav.helse.flex.refactor.*
import org.springframework.stereotype.Component

@Component
class Identer(
    private val pdlClient: PdlClient,
    private val oppgaveClient: OppgaveClient
) {
    private val log = logger()

    fun hentIdenterFraPDL(journalpost: Journalpost): List<PdlIdent> {
        if (journalpost.bruker == null) {
            log.info("Bruker er ikke satt på journalpost: ${journalpost.journalpostId}. Kan ikke hente fra PDL")
            throw OpprettManuellOppgaveException()
        }

        if (journalpost.bruker.isORGNR) {
            log.info("Kaller ikke PDL da bruker på journalpost ${journalpost.journalpostId} er orgnummer")
            return emptyList()
        }

        try {
            log.info("Henter alle identer for bruker på journalpost: ${journalpost.journalpostId} fra PDL")
            return pdlClient.hentIdenterForJournalpost(journalpost)
        } catch (e: FinnerIkkePersonException) {
            if (oppgaveClient.finnesOppgaveForJournalpost(journalpost.journalpostId)) {
                log.info("Finner ikke person i PDL og journalpost ${journalpost.journalpostId} har oppgave.")
                throw OpprettManuellOppgaveException()
            } else {
                throw e
            }
        }
    }
}
