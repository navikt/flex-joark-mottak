package no.nav.helse.flex.journalpost

import no.nav.helse.flex.felleskodeverk.FkvClient
import org.springframework.stereotype.Component

@Component
class BrevkodeMapper(
    private val fkvClient: FkvClient
) {
    fun mapBrevkodeTilTemaOgType(journalpost: Journalpost): Journalpost {
        val behandlingstema = if (journalpost.behandlingstema.isNullOrEmpty()) {
            fkvClient.hentKrutkoder().getBehandlingstema(journalpost.tema, journalpost.brevkode)
        } else {
            journalpost.behandlingstema
        }
        val behandlingstype = fkvClient.hentKrutkoder().getBehandlingstype(journalpost.tema, journalpost.brevkode)

        return journalpost.copy(behandlingstema = behandlingstema, behandlingstype = behandlingstype)
    }
}
