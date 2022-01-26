package no.nav.helse.flex.infrastructure.kafka

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KafkaEvent(
    private val hendelsesId: String,
    private val hendelsesType: String,
    private val journalpostId: Long,
    val temaNytt: String,
    val mottaksKanal: String,
    val journalpostStatus: String
) {
    private val versjon = 0
    private val temaGammelt: String? = null
    private val kanalReferanseId: String? = null
    private val behandlingstema: String? = null

    fun getJournalpostId(): String {
        return journalpostId.toString()
    }
}
