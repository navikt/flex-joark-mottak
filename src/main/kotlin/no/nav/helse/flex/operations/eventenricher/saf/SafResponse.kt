package no.nav.helse.flex.operations.eventenricher.saf

import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost

data class SafResponse(
    val data: Data
)

data class Data(
    val journalpost: Journalpost
)
