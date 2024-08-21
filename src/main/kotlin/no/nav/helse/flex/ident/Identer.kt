package no.nav.helse.flex.ident

import no.nav.helse.flex.journalpost.FinnerIkkePersonException
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.OpprettManuellOppgaveException
import no.nav.helse.flex.logger
import no.nav.helse.flex.oppgave.OppgaveClient
import org.springframework.stereotype.Component

@Component
class Identer(
    private val pdlClient: PdlClient,
    private val oppgaveClient: OppgaveClient,
) {
    private val log = logger()

    fun hentIdenterFraPDL(journalpost: Journalpost): List<PdlIdent> {
        if (journalpost.bruker == null) {
            log.info("Bruker er ikke satt på journalpost: ${journalpost.journalpostId}. Kan ikke hente fra PDL.")
            throw OpprettManuellOppgaveException()
        }

        if (journalpost.bruker.isORGNR) {
            log.info("Bruker på journalpost: ${journalpost.journalpostId} er orgnummer. Henter ikke fra PDL.")
            return emptyList()
        }

        try {
            return pdlClient.hentIdenterForJournalpost(journalpost)
        } catch (e: FinnerIkkePersonException) {
            if (oppgaveClient.finnesOppgaveForJournalpost(journalpost.journalpostId)) {
                log.info(
                    "Fant ikke person i PDL, men det finnes allerede en oppgave for " +
                        "journalpost: ${journalpost.journalpostId}.",
                )
                throw OpprettManuellOppgaveException()
            } else {
                throw e
            }
        }
    }
}
