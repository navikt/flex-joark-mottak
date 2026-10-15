package no.nav.helse.flex.journalpost

import no.nav.helse.flex.felleskodeverk.KodeverkClient
import no.nav.helse.flex.logger
import org.springframework.stereotype.Component

@Component
class BrevkodeMapper(
    private val kodeverkClient: KodeverkClient,
) {
    private val log = logger()

    fun mapBrevkodeTilTemaOgType(journalpost: Journalpost): Journalpost {
        val behandlingstema =
            if (journalpost.behandlingstema.isNullOrEmpty()) {
                log.debug(
                    "Debug: Behandlingstema for journalpost: ${journalpost.journalpostId} med brevkode: ${journalpost.brevkode} " +
                        "og tema: ${journalpost.tema} er null eller tom, mapper til behandlingstema fra kodeverk.",
                )
                kodeverkClient.hentKrutkoder().getBehandlingstema(journalpost.tema, journalpost.brevkode)
            } else {
                journalpost.behandlingstema
            }
        val behandlingstype = kodeverkClient.hentKrutkoder().getBehandlingstype(journalpost.tema, journalpost.brevkode)

        log.info(
            "Mappet journalpost: ${journalpost.journalpostId} med brevkode: ${journalpost.brevkode} " +
                "tema: ${journalpost.tema} til behandlingstema: $behandlingstema og behandlingstype: $behandlingstype.",
        )
        return journalpost.copy(behandlingstema = behandlingstema, behandlingstype = behandlingstype)
    }
}
