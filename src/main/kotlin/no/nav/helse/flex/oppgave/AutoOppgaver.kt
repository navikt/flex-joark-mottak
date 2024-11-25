package no.nav.helse.flex.oppgave

import no.nav.helse.flex.felleskodeverk.SkjemaMetadata
import no.nav.helse.flex.ident.AKTORID
import no.nav.helse.flex.ident.FOLKEREGISTERIDENT
import no.nav.helse.flex.ident.Identer
import no.nav.helse.flex.journalpost.BrevkodeMapper
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.logger
import org.springframework.stereotype.Component

@Component
class AutoOppgaver(
    private val identer: Identer,
    private val oppgaveClient: OppgaveClient,
    private val brevkodeMapper: BrevkodeMapper,
) {
    private val log = logger()

    fun opprettOppgave(jp: Journalpost): Journalpost {
        var journalpost = brevkodeMapper.mapBrevkodeTilTemaOgType(jp)
        val identer = identer.hentIdenterFraPDL(journalpost)
        val oppgavetype = SkjemaMetadata.getOppgavetype(journalpost.tema, journalpost.brevkode)
        val frist = SkjemaMetadata.getFrist(journalpost.tema, journalpost.brevkode)

        val requestData =
            OppgaveRequest(
                aktoerId = identer.first { it.gruppe == AKTORID }.ident,
                journalpostId = journalpost.journalpostId,
                tema = journalpost.tema,
                behandlingstema = journalpost.behandlingstema,
                behandlingstype = journalpost.behandlingstype,
                oppgavetype = oppgavetype,
                frist = frist,
            )
        if (!journalpost.journalforendeEnhet.isNullOrBlank()) {
            requestData.tildeltEnhetsnr = journalpost.journalforendeEnhet
        }

        oppgaveClient.opprettOppgave(requestData)
        log.info("Opprettet oppgave for journalpost: ${journalpost.journalpostId}.")

        journalpost = journalpost.copy(sak = Journalpost.Sak("GENERELL_SAK"))

        if (journalpost.avsenderMottaker?.id == null) {
            log.info("Setter bruker som avsender på journalpost: ${journalpost.journalpostId}.")
            journalpost =
                journalpost.copy(
                    avsenderMottaker =
                        Journalpost.AvsenderMottaker(
                            identer.first {
                                it.gruppe == FOLKEREGISTERIDENT
                            }.ident,
                            "FNR",
                        ),
                )
        }

        return journalpost
    }
}
