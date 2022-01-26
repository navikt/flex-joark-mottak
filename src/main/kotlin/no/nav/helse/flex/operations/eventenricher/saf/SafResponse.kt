package no.nav.helse.flex.operations.eventenricher.saf

import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import java.io.Serializable

class SafResponse : Serializable {
    var data: Data? = null

    inner class Data : Serializable {
        var journalpost: Journalpost? = null
    }
}
