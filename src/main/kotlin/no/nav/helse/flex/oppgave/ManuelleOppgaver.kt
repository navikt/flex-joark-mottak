package no.nav.helse.flex.oppgave

import no.nav.helse.flex.ident.AKTORID
import no.nav.helse.flex.ident.Identer
import no.nav.helse.flex.ident.PdlIdent
import no.nav.helse.flex.journalpost.BrevkodeMapper
import no.nav.helse.flex.journalpost.Journalpost
import no.nav.helse.flex.journalpost.OpprettManuellOppgaveException
import no.nav.helse.flex.journalpost.SafClient
import no.nav.helse.flex.logger
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException

private const val FORDELINGSOPPGAVE = "FDR"
private const val JOURNALORINGSOPPGAVE = "JFR"
private const val TEMA_UKJENT = "UKJ"
private const val TEMA_GENERELL = "GEN"

@Component
class ManuelleOppgaver(
    private val oppgaveClient: OppgaveClient,
    private val safClient: SafClient,
    private val brevkodeMapper: BrevkodeMapper,
    private val identerService: Identer,
) {
    private val log = logger()

    fun opprettOppgave(journalpostId: String) {
        if (oppgaveClient.finnesOppgaveForJournalpost(journalpostId)) {
            log.info("Avslutter behandling da journalpost: $journalpostId allerede har en oppgave.")
            return
        }

        val journalpost = safClient.hentJournalpost(journalpostId)
        val identer =
            try {
                identerService.hentIdenterFraPDL(journalpost)
            } catch (_: OpprettManuellOppgaveException) {
                emptyList()
            }

        if (isJournalpostToFordeling(journalpost)) {
            createFordelingsoppgave(journalpost, identer)
        } else {
            createManuellJournalfoeringsoppgave(journalpost, identer)
        }
    }

    private fun isJournalpostToFordeling(journalpost: Journalpost): Boolean {
        if (journalpost.tema != "SYK") {
            log.error(
                "Journalpost ${journalpost.journalpostId} har tema ${journalpost.tema}. Denne applikasjonen støtter " +
                    "bare tema: SYK. Oppretter fordelingsoppgave",
            )
            return true
        }
        return false
    }

    private fun createFordelingsoppgave(
        journalpost: Journalpost,
        identer: List<PdlIdent>,
    ) {
        val requestData =
            OppgaveRequest(
                journalpostId = journalpost.journalpostId,
                oppgavetype = FORDELINGSOPPGAVE,
                frist = 1,
            ).apply {
                if (journalpost.bruker?.isAktoerId == true || journalpost.bruker?.isFNR == true) {
                    aktoerId = identer.first { it.gruppe == AKTORID }.ident
                } else if (journalpost.bruker?.isORGNR == true) {
                    orgnr = journalpost.bruker.id
                }

                tema =
                    if (journalpost.tema == TEMA_UKJENT) {
                        TEMA_GENERELL
                    } else {
                        journalpost.tema
                    }
            }

        oppgaveClient.opprettOppgave(requestData)
    }

    private fun createManuellJournalfoeringsoppgave(
        journalpost: Journalpost,
        identer: List<PdlIdent>,
    ) {
        val mappetJournalpost =
            runCatching {
                return@runCatching brevkodeMapper.mapBrevkodeTilTemaOgType(journalpost)
            }.recover {
                if (it is IllegalArgumentException) {
                    if (journalpost.journalforendeEnhet.isNullOrEmpty()) {
                        log.info(
                            "Fant ikke behandlingsverdier for journalpost: ${journalpost.journalpostId} med " +
                                "tema: ${journalpost.tema} og brevkode: ${journalpost.brevkode}.",
                        )
                        return@recover journalpost
                    } else {
                        log.info(
                            "Fant ikke behandlingsverdier for journalpost: ${journalpost.journalpostId} med " +
                                "tema: ${journalpost.tema} og brevkode: ${journalpost.brevkode} i resultat fra " +
                                "journalforendeEnhet: ${journalpost.journalforendeEnhet}.",
                        )
                        return@recover journalpost
                    }
                }
                throw it
            }.getOrThrow()

        val requestData =
            OppgaveRequest(
                journalpostId = mappetJournalpost.journalpostId,
                tema = mappetJournalpost.tema,
                behandlingstema = mappetJournalpost.behandlingstema,
                behandlingstype = mappetJournalpost.behandlingstype,
                oppgavetype = JOURNALORINGSOPPGAVE,
                tildeltEnhetsnr = mappetJournalpost.journalforendeEnhet,
                beskrivelse = mappetJournalpost.tittel,
                frist = 1,
            ).apply {
                if (mappetJournalpost.bruker?.isAktoerId == true || mappetJournalpost.bruker?.isFNR == true) {
                    aktoerId = identer.first { it.gruppe == AKTORID }.ident
                } else if (mappetJournalpost.bruker?.isORGNR == true) {
                    orgnr = mappetJournalpost.bruker.id
                }
            }

        oppgaveClient.opprettOppgave(requestData)
    }
}
