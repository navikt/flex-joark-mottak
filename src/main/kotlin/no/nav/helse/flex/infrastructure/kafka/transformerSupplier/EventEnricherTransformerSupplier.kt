package no.nav.helse.flex.infrastructure.kafka.transformerSupplier

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException
import no.nav.helse.flex.infrastructure.exceptions.InvalidJournalpostStatusException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incRetry
import no.nav.helse.flex.infrastructure.metrics.Metrics.setRetrystoreGauge
import no.nav.helse.flex.operations.eventenricher.EventEnricher
import no.nav.helse.flex.operations.eventenricher.pdl.FinnerIkkePersonException
import no.nav.helse.flex.operations.generell.oppgave.OppgaveClient
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.kstream.TransformerSupplier
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration

class EventEnricherTransformerSupplier(
    private val stateStoreName: String,
    private val eventEnricher: EventEnricher = EventEnricher(),
    private val oppgaveClient: OppgaveClient = OppgaveClient()
) : TransformerSupplier<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {

    override fun get(): Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
        return object : Transformer<String, KafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
            private lateinit var stateStore: KeyValueStore<String, EnrichedKafkaEvent>

            override fun init(context: ProcessorContext) {
                stateStore = context.getStateStore(stateStoreName)

                context.schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME) {
                    stateStore.all().forEachRemaining { keyValue: KeyValue<String, EnrichedKafkaEvent> ->
                        val enrichedKafkaEvent = keyValue.value
                        val id = keyValue.key
                        val completeSendToStream = doOperations(enrichedKafkaEvent)
                        if (completeSendToStream) {
                            stateStore.delete(id)
                            context.forward(id, enrichedKafkaEvent)
                            context.commit()
                        } else {
                            stateStore.put(id, enrichedKafkaEvent)
                        }
                    }
                    setRetrystoreGauge(stateStoreName, stateStore.approximateNumEntries())
                }
            }

            override fun transform(id: String, kafkaEvent: KafkaEvent?): KeyValue<String, EnrichedKafkaEvent>? {
                val enrichedKafkaEvent = EnrichedKafkaEvent(kafkaEvent!!)
                var keyValue: KeyValue<String, EnrichedKafkaEvent>? = null
                val sendToStream = doOperations(enrichedKafkaEvent)

                if (sendToStream) {
                    keyValue = KeyValue.pair(id, enrichedKafkaEvent)
                } else if (!enrichedKafkaEvent.isToIgnore) {
                    stateStore.putIfAbsent(id, enrichedKafkaEvent)
                    keyValue = null
                    setRetrystoreGauge(stateStoreName, stateStore.approximateNumEntries())
                }
                return keyValue
            }

            private fun doOperations(enrichedKafkaEvent: EnrichedKafkaEvent): Boolean {
                return try {
                    MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
                    eventEnricher.createEnrichedKafkaEvent(enrichedKafkaEvent)
                    true
                } catch (e: Exception) {
                    return when (e) {
                        is InvalidJournalpostStatusException -> {
                            enrichedKafkaEvent.isToIgnore = true
                            true
                        }

                        is FunctionalRequirementException -> {
                            enrichedKafkaEvent.isToManuell = true
                            true
                        }

                        is ExternalServiceException,
                        is TemporarilyUnavailableException -> {
                            enrichedKafkaEvent.incNumFailedAttempts()
                            if (enrichedKafkaEvent.numFailedAttempts < MAX_NUM_RETRY) {
                                incRetry(stateStoreName, enrichedKafkaEvent)
                                log.info("Feilet under berikelse av journalpost ${enrichedKafkaEvent.journalpostId} for gang nummer ${enrichedKafkaEvent.numFailedAttempts}. Forsøker på nytt senere")
                                false
                            } else {
                                log.info("Feilet under berikelse av journalpost ${enrichedKafkaEvent.journalpostId} for gang nummer ${enrichedKafkaEvent.numFailedAttempts}. Gir opp videre behandling")
                                enrichedKafkaEvent.isToManuell = true
                                true
                            }
                        }

                        is FinnerIkkePersonException -> {
                            // TODO: Lag test som sjekker denne logikken.
                            log.warn("Finner ikke person for journalpost ${enrichedKafkaEvent.journalpostId}. Sjekker om det finnes oppgave.")
                            return if (oppgaveClient.checkIfJournapostHasOppgave(enrichedKafkaEvent.journalpostId)) {
                                log.info("Journalpost ${enrichedKafkaEvent.journalpostId} har oppgave.")
                                true
                            } else {
                                log.info("Journalpost ${enrichedKafkaEvent.journalpostId} har ikke oppgave. Forsøker på nytt senere")
                                false
                            }
                        }

                        else -> {
                            log.error(
                                "Uventet feil på journalpost ${enrichedKafkaEvent.journalpostId}. Forsøker på nytt senere",
                                e
                            )
                            false
                        }
                    }
                } finally {
                    MDC.clear()
                }
            }

            override fun close() {
                // Note: The store should NOT be closed manually here via `stateStore.close()`!
                // The Kafka Streams API will automatically close stores when necessary.
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(EventEnricherTransformerSupplier::class.java)
        private const val MAX_NUM_RETRY = 48
    }
}
