package no.nav.helse.flex.infrastructure.exceptions

class ExternalServiceException(
    val feilmelding: String,
    val feilkode: Int
) : Exception(feilmelding)
