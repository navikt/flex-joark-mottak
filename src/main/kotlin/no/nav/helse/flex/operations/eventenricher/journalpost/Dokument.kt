package no.nav.helse.flex.operations.eventenricher.journalpost

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class Dokument(val brevkode: String?, val tittel: String? = null, val dokumentInfoId: String)
