package no.nav.helse.flex.operations.eventenricher.saf

import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import java.io.Serializable

class SafResponse(
    val data: Data
) : Serializable {

    inner class Data(
        var journalpost: Journalpost
    ) : Serializable
}
