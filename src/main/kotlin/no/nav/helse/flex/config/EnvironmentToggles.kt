package no.nav.helse.flex.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class EnvironmentToggles(
    @Value("\${NAIS_CLUSTER_NAME}") private val naisCluster: String,
    @Value("\${AIVEN_DOKUMENT_TOPIC}") private val dokumentTopic: String,
) {
    fun isProduction() = "prod-gcp" == naisCluster

    @Suppress("unused") // brukes for å dynamisk hente topic navn
    fun dokumentTopic() = dokumentTopic
}
