package no.nav.helse.flex.journalpost

import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.felleskodeverk.SkjemaMetadata
import no.nav.helse.flex.logger
import no.nav.helse.flex.oppgave.AutoOppgaver
import no.nav.helse.flex.oppgave.ManuelleOppgaver
import no.nav.helse.flex.oppgave.OppgaveClient
import org.springframework.stereotype.Component

@Component
class JournalpostBehandler(
    private val manuelleOppgaver: ManuelleOppgaver,
    private val safClient: SafClient,
    private val oppgaveClient: OppgaveClient,
    private val autoOppgaver: AutoOppgaver,
    private val dokArkivClient: DokArkivClient,
) {
    private val log = logger()

    fun behandleJournalpost(kafkaEvent: KafkaEvent) {
        runCatching {
            var journalpost = hentJournalpost(kafkaEvent.journalpostId)

            if (!SkjemaMetadata.inAutoList(journalpost.tema, journalpost.brevkode)) {
                throw OpprettManuellOppgaveException()
            }

            if (!gyldigeDokumentTitler(journalpost)) {
                throw OpprettManuellOppgaveException()
            }

            if (oppgaveClient.finnesOppgaveForJournalpost(journalpost.journalpostId)) {
                log.info("Det finnes oppgave på journalpost ${journalpost.journalpostId}, avslutt videre behandling")
                return
            }

            journalpost = autoOppgaver.opprettOppgave(journalpost)

            dokArkivClient.updateJournalpost(journalpost)
            log.info("Oppdaterte journalpost: $journalpost")

            dokArkivClient.ferdigstillJournalpost(journalpost.journalpostId)
            log.info("Ferdigstilt og fullført behandling av journalpost ${journalpost.journalpostId}")
        }.recoverCatching { exception ->
            when (exception) {
                is OpprettManuellOppgaveException -> {
                    manuelleOppgaver.opprettOppgave(kafkaEvent.journalpostId)
                    return
                }
                is InvalidJournalpostStatusException -> {
                    // OK - skal ikke journalføre disse
                    return
                }
                else -> {
                    throw exception
                }
            }
        }.onFailure { exception ->
            throw exception
        }
    }

    private fun hentJournalpost(journalpostId: String): Journalpost {
        val journalpost: Journalpost = safClient.hentJournalpost(journalpostId)

        log.info("Hentet journalpost: $journalpost")

        if (journalpost.invalidJournalpostStatus()) {
            log.info("Avslutter videre behandling da journalpost ${journalpost.journalpostId} har status ${journalpost.journalstatus}")
            throw InvalidJournalpostStatusException()
        }
        if (SkjemaMetadata.isIgnoreskjema(journalpost.tema, journalpost.brevkode)) {
            log.info(
                "Avslutter videre behandling da journalpost ${journalpost.journalpostId} har brevkode " +
                    "${journalpost.brevkode} på tema ${journalpost.tema} som eksplisitt skal ignoreres!",
            )
            throw InvalidJournalpostStatusException()
        }

        return journalpost
    }

    private fun gyldigeDokumentTitler(journalpost: Journalpost): Boolean {
        for (dokument in journalpost.dokumenter) {
            if (dokument.tittel.isNullOrEmpty()) {
                log.info(
                    "Avbryter automatisk behandling. Journalpost ${journalpost.journalpostId} har dokument " +
                        "${dokument.dokumentInfoId} uten tittel",
                )
                return false
            }
        }
        return true
    }
}
