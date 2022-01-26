package no.nav.helse.flex.infrastructure.kafka

import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import no.nav.helse.flex.operations.generell.oppgave.Oppgave
import java.util.*

data class EnrichedKafkaEvent(
    val kafkaEvent: KafkaEvent,
    var journalpost: Journalpost? = null,
    var oppgave: Oppgave? = null,
    val saksId: String? = null,
    var identer: List<Ident>? = null
) {
    val correlationId = UUID.randomUUID().toString()
    var numFailedAttempts = 0
    var isToManuell = false
    var isToIgnore = false
    var toFordeling = false

    fun incNumFailedAttempts() {
        numFailedAttempts += 1
    }

    val journalpostId: String
        get() = kafkaEvent.getJournalpostId()

    val skjema: String?
        get() = if (journalpost != null) {
            journalpost!!.brevkode
        } else null

    val tema: String
        get() = if (journalpost == null) {
            kafkaEvent.temaNytt
        } else journalpost!!.tema

    val fnr: String?
        get() {
            if (journalpost!!.bruker!!.isFNR) {
                return journalpost!!.bruker!!.id
            } else {
                for (ident in identer!!) {
                    if (ident.isFNR) return ident.iD
                }
            }
            return null
        }

    val aktoerId: String?
        get() {
            if (journalpost!!.bruker!!.isAktoerId) {
                return journalpost!!.bruker!!.id
            } else {
                for (ident in identer!!) {
                    if (ident.isAktoerId) return ident.iD
                }
            }
            return null
        }

    val isPersonbruker: Boolean
        get() {
            if (journalpost?.bruker == null) {
                return false
            }
            return !journalpost!!.bruker!!.isORGNR
        }

    fun withSetToManuell(toManuell: Boolean): EnrichedKafkaEvent {
        isToManuell = toManuell
        return this
    }

    fun hasOppgave(): Boolean {
        return oppgave != null
    }

    fun getOppgaveId(): String? {
        return oppgave?.id
    }
}
