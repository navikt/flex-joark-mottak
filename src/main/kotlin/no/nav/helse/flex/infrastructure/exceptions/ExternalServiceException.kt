package no.nav.helse.flex.infrastructure.exceptions

import java.lang.Exception

class ExternalServiceException(feilmelding: String, feilkode: Int) : Exception(
    feilmelding
) {
    private val feilkode: String

    init {
        this.feilkode = feilkode.toString()
    }
}
