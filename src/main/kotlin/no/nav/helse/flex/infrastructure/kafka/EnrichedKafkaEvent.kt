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
    var identer: List<Ident> = emptyList()
) {
    val correlationId = UUID.randomUUID().toString()
    var numFailedAttempts = 0
    var isToManuell = false
    var isToIgnore = false

    fun incNumFailedAttempts() {
        numFailedAttempts += 1
    }

    val journalpostId: String
        get() = kafkaEvent.getJournalpostId()

    val skjema: String?
        get() = if (journalpost != null) {
            journalpost!!.brevkode
        } else {
            null
        }

    val tema: String
        get() = if (journalpost == null) {
            kafkaEvent.temaNytt
        } else {
            journalpost!!.tema
        }

    val fnr: String?
        get() {
            if (journalpost?.bruker?.isFNR == true) {
                return journalpost!!.bruker!!.id
            } else {
                for (ident in identer) {
                    if (ident.isFNR) return ident.ident
                }
            }
            return null
        }

    val aktoerId: String?
        get() {
            if (journalpost?.bruker?.isAktoerId == true) {
                return journalpost!!.bruker!!.id
            } else {
                for (ident in identer) {
                    if (ident.isAktoerId) return ident.ident
                }
            }
            return null
        }

    val isPersonbruker: Boolean
        get() {
            return journalpost?.bruker?.isORGNR != true
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

    fun getId(): String? {
        return if (isPersonbruker) {
            aktoerId
        } else if (journalpost?.bruker?.isORGNR == true) {
            journalpost?.bruker?.id
        } else {
            null
        }
    }
}
