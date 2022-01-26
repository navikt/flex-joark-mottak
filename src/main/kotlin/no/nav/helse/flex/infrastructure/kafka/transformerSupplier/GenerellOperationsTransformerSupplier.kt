package no.nav.helse.flex.infrastructure.kafka.transformerSupplier

import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException
import no.nav.helse.flex.infrastructure.exceptions.FunctionalRequirementException
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.metrics.Metrics.incRetry
import no.nav.helse.flex.infrastructure.metrics.Metrics.setRetrystoreGauge
import no.nav.helse.flex.operations.generell.GenerellOperations
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.kstream.TransformerSupplier
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.Duration

class GenerellOperationsTransformerSupplier(
    private val stateStoreName: String,
    private val generellOperations: GenerellOperations = GenerellOperations()
) : TransformerSupplier<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {

    override fun get(): Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
        return object : Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
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

            override fun transform(
                id: String,
                enrichedKafkaEvent: EnrichedKafkaEvent
            ): KeyValue<String, EnrichedKafkaEvent>? {
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
                    generellOperations.executeProcess(enrichedKafkaEvent)
                    true
                } catch (e: FunctionalRequirementException) {
                    enrichedKafkaEvent.isToManuell = true
                    true
                } catch (e: TemporarilyUnavailableException) {
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
                } catch (e: ExternalServiceException) {
                    enrichedKafkaEvent.isToManuell = true
                    true
                } catch (e: Exception) {
                    log.error("Uventet feil på journalpost ${enrichedKafkaEvent.journalpostId}", e)
                    enrichedKafkaEvent.isToManuell = true
                    true
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
        private val log = LoggerFactory.getLogger(GenerellOperationsTransformerSupplier::class.java)
        private const val MAX_NUM_RETRY = 5
    }
}
