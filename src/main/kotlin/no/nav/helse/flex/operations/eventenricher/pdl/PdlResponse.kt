package no.nav.helse.flex.operations.eventenricher.pdl

import java.io.Serializable

class PdlResponse : Serializable {
    var data: Data? = null
    val identer: HentIdenter?
        get() = data?.identer

    inner class Data : Serializable {
        var identer: HentIdenter? = null
    }
}
