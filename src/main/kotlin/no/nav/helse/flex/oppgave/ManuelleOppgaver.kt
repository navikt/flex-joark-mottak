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
            log.info("Det finnes oppgave på journalpost $journalpostId, avslutt videre behandling")
            return
        }

        val journalpost = safClient.hentJournalpost(journalpostId)
        val identer =
            try {
                identerService.hentIdenterFraPDL(journalpost)
            } catch (e: OpprettManuellOppgaveException) {
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
                "Journalpost ${journalpost.journalpostId} har tema ${journalpost.tema} og skal ikke skje, oppretter fordelingsoppgave",
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
        jp: Journalpost,
        identer: List<PdlIdent>,
    ) {
        val journalpost =
            runCatching {
                val jpMedBehandlingsverdier = brevkodeMapper.mapBrevkodeTilTemaOgType(jp)
                log.info(
                    "Setter følgende verdier behandlingstema: '${jpMedBehandlingsverdier.behandlingstema}', " +
                        "behandlingstype: '${jpMedBehandlingsverdier.behandlingstype}' på " +
                        "journalpost ${jpMedBehandlingsverdier.journalpostId}",
                )
                return@runCatching jpMedBehandlingsverdier
            }.recover {
                if (it is IllegalArgumentException) {
                    if (jp.journalforendeEnhet.isNullOrEmpty()) {
                        log.info(
                            "Klarte ikke finne behandlingsverdier for journalpost ${jp.journalpostId} med tema " +
                                "${jp.tema} og brevkode ${jp.brevkode}",
                        )
                        return@recover jp
                    } else {
                        log.info(
                            "Klarte ikke finne behandlingsverdier for journalpost ${jp.journalpostId} med tema " +
                                "${jp.tema} og brevkode ${jp.brevkode}, bruker journalforendeEnhet ${jp.journalforendeEnhet}",
                        )
                        return@recover jp
                    }
                }
                throw it
            }.getOrThrow()

        val requestData =
            OppgaveRequest(
                journalpostId = journalpost.journalpostId,
                tema = journalpost.tema,
                behandlingstema = journalpost.behandlingstema,
                behandlingstype = journalpost.behandlingstype,
                oppgavetype = JOURNALORINGSOPPGAVE,
                tildeltEnhetsnr = journalpost.journalforendeEnhet,
                beskrivelse = journalpost.tittel,
                frist = 1,
            ).apply {
                if (journalpost.bruker?.isAktoerId == true || journalpost.bruker?.isFNR == true) {
                    aktoerId = identer.first { it.gruppe == AKTORID }.ident
                } else if (journalpost.bruker?.isORGNR == true) {
                    orgnr = journalpost.bruker.id
                }
            }

        oppgaveClient.opprettOppgave(requestData)
    }
}
