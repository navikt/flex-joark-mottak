package no.nav.helse.flex.oppgave

import no.nav.helse.flex.felleskodeverk.FkvClient
import no.nav.helse.flex.ident.AKTORID
import no.nav.helse.flex.ident.PdlClient
import no.nav.helse.flex.ident.PdlIdent
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.SafClient
import no.nav.helse.flex.logger
import no.nav.helse.flex.refactor.ExternalServiceException
import org.springframework.stereotype.Component

private const val FORDELINGSOPPGAVE = "FDR"
private const val JOURNALORINGSOPPGAVE = "JFR"
private const val TEMA_UKJENT = "UKJ"
private const val TEMA_GENERELL = "GEN"

@Component
class ManuelleOppgaver(
    private val oppgaveClient: OppgaveClient,
    private val pdlClient: PdlClient,
    private val safClient: SafClient,
    private val fkvClient: FkvClient
) {
    private val log = logger()

    fun opprettOppgave(journalpostId: String) {
        if (oppgaveClient.finnesOppgaveForJournalpost(journalpostId)) {
            log.info("Det finnes oppgave på journalpost $journalpostId, avslutt videre behandling")
            return
        }

        val journalpost = safClient.hentJournalpost(journalpostId)
        val identer = pdlClient.hentIdenterForJournalpost(journalpost)

        if (isJournalpostToFordeling(journalpost)) {
            createFordelingsoppgave(journalpost, identer)
        } else {
            createManuellJournalfoeringsoppgave(journalpost, identer)
        }
    }

    private fun isJournalpostToFordeling(journalpost: Journalpost): Boolean {
        if (journalpost.tema != "SYK") {
            log.error("Journalpost ${journalpost.journalpostId} har tema ${journalpost.tema} og skal ikke skje, oppretter fordelingsoppgave")
            return true
        }

        return false
    }

    private fun createFordelingsoppgave(journalpost: Journalpost, identer: List<PdlIdent>) {
        val requestData = OppgaveRequest(
            journalpostId = journalpost.journalpostId,
            oppgavetype = FORDELINGSOPPGAVE,
            frist = 1
        ).apply {
            if (journalpost.bruker?.isORGNR != true) {
                aktoerId = identer.first { it.gruppe == AKTORID }.ident
            } else {
                orgnr = journalpost.bruker.id
            }

            tema = if (journalpost.tema == TEMA_UKJENT) {
                TEMA_GENERELL
            } else {
                journalpost.tema
            }
        }

        oppgaveClient.createOppgave(requestData)
    }

    private fun createManuellJournalfoeringsoppgave(journalpost: Journalpost, identer: List<PdlIdent>) {
        val behandlingstema = fkvClient.hentKrutkoder().getBehandlingstema(journalpost.tema, journalpost.brevkode)
        val behandlingstype = if (journalpost.behandlingstema.isNullOrEmpty()) fkvClient.hentKrutkoder().getBehandlingstype(journalpost.tema, journalpost.brevkode) else journalpost.behandlingstema

        log.info("Setter følgende verdier behandlingstema: '$behandlingstema', behandlingstype: '$behandlingstype' på journalpost ${journalpost.journalpostId}")

        val requestData = OppgaveRequest(
            journalpostId = journalpost.journalpostId,
            tema = journalpost.tema,
            behandlingstema = behandlingstema,
            behandlingstype = behandlingstype,
            oppgavetype = JOURNALORINGSOPPGAVE,
            tildeltEnhetsnr = journalpost.journalforendeEnhet,
            beskrivelse = journalpost.tittel,
            frist = 1
        ).apply {
            if (journalpost.bruker?.isORGNR != true) {
                aktoerId = identer.first { it.gruppe == AKTORID }.ident
            } else {
                orgnr = journalpost.bruker.id
            }
        }

        try {
            oppgaveClient.createOppgave(requestData)
        } catch (e: ExternalServiceException) {
            if (isErrorInvalidEnhet(e.feilmelding)) {
                log.warn("Klarte ikke opprette oppgave pga ugyldig enhet på journalpost ${journalpost.journalpostId}")
                requestData.removeJournalforendeEnhet()
                oppgaveClient.createOppgave(requestData)
            } else if (isErrorInvalidOrgNr(e.feilmelding)) {
                log.warn("Klarte ikke opprette oppgave pga ugyldig OrgNr på journalpost ${journalpost.journalpostId}")
                requestData.removeOrgNr()
                oppgaveClient.createOppgave(requestData)
            } else {
                throw e
            }
        }
    }

    private fun isErrorInvalidEnhet(feilmelding: String): Boolean {
        return (
            feilmelding.contains("NAVEnheten '") && feilmelding.contains("' er av typen oppgavebehandler") ||
                feilmelding.contains("NAVEnheten '") && feilmelding.contains("' har status: 'Nedlagt'") ||
                feilmelding.contains("Enheten med nummeret '") && feilmelding.contains("' eksisterer ikke")
            )
    }

    private fun isErrorInvalidOrgNr(feilmelding: String?): Boolean {
        return (feilmelding != null && feilmelding.contains("Organisasjonsnummer er ugyldig"))
    }
}
