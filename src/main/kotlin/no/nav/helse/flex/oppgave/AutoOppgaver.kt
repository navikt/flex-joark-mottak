package no.nav.helse.flex.oppgave

import no.nav.helse.flex.felleskodeverk.FkvClient
import no.nav.helse.flex.ident.AKTORID
import no.nav.helse.flex.ident.FOLKEREGISTERIDENT
import no.nav.helse.flex.ident.Identer
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.logger
import no.nav.helse.flex.refactor.SkjemaMetadata
import org.springframework.stereotype.Component

@Component
class AutoOppgaver(
    private val identer: Identer,
    private val fkvClient: FkvClient,
    private val oppgaveClient: OppgaveClient
) {
    private val log = logger()

    fun opprettOppgave(jp: Journalpost): Journalpost {
        var journalpost = jp
        val identer = identer.hentIdenterFraPDL(journalpost)

        val behandlingstema = fkvClient.hentKrutkoder().getBehandlingstema(journalpost.tema, journalpost.brevkode)
        val behandlingstype = if (journalpost.behandlingstema.isNullOrEmpty()) fkvClient.hentKrutkoder().getBehandlingstype(journalpost.tema, journalpost.brevkode) else journalpost.behandlingstema
        val oppgavetype = SkjemaMetadata.getOppgavetype(journalpost.tema, journalpost.brevkode)
        val frist = SkjemaMetadata.getFrist(journalpost.tema, journalpost.brevkode)

        log.info("Setter følgende verdier behandlingstema: '$behandlingstema', behandlingstype: '$behandlingstype' og oppgavetype: '$oppgavetype' på journalpost ${journalpost.journalpostId}")
        journalpost = journalpost.copy(behandlingstema = behandlingstema, behandlingstype = behandlingstype)

        val requestData = OppgaveRequest(
            aktoerId = identer.first { it.gruppe == AKTORID }.ident,
            journalpostId = journalpost.journalpostId,
            tema = journalpost.tema,
            behandlingstema = journalpost.behandlingstema,
            behandlingstype = journalpost.behandlingstype,
            oppgavetype = oppgavetype,
            frist = frist
        )
        if (!journalpost.journalforendeEnhet.isNullOrBlank()) {
            requestData.tildeltEnhetsnr = journalpost.journalforendeEnhet
        }

        log.info("Oppretter oppgave for journalpost: ${journalpost.journalpostId}")
        oppgaveClient.createOppgave(requestData)

        journalpost = journalpost.copy(sak = Journalpost.Sak("GENERELL_SAK"))

        if (journalpost.avsenderMottaker?.id == null) {
            log.info("Setter bruker som avsender på journalpost: ${journalpost.journalpostId}")
            journalpost = journalpost.copy(avsenderMottaker = Journalpost.AvsenderMottaker(identer.first { it.gruppe == FOLKEREGISTERIDENT }.ident, "FNR"))
        }

        return journalpost
    }
}
