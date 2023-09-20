package no.nav.helse.flex.refactor

class InvalidJournalpostStatusException : Exception()
class ExternalServiceException(
    val feilmelding: String,
    val feilkode: Int
) : Exception(feilmelding)
class TemporarilyUnavailableException : Exception()
class FinnerIkkePersonException : Exception()
class OpprettManuellOppgaveException : Exception()
