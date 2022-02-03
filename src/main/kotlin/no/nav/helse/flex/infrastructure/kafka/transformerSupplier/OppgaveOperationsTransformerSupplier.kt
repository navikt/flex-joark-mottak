package no.nav.helse.flex.infrastructure.kafka.transformerSupplier

import no.nav.helse.flex.infrastructure.MDCConstants
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.operations.manuell.ManuellOperations
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Transformer
import org.apache.kafka.streams.kstream.TransformerSupplier
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.lang.Exception
import java.time.Duration

class OppgaveOperationsTransformerSupplier(
    private val stateStoreName: String,
    private val manuellOperation: ManuellOperations = ManuellOperations()
) : TransformerSupplier<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {

    override fun get(): Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
        return object : Transformer<String, EnrichedKafkaEvent, KeyValue<String, EnrichedKafkaEvent>> {
            private lateinit var stateStore: KeyValueStore<String, EnrichedKafkaEvent>

            override fun init(context: ProcessorContext) {
                stateStore = context.getStateStore(stateStoreName)

                context.schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME) {
                    stateStore.all().forEachRemaining { keyValue: KeyValue<String, EnrichedKafkaEvent> ->
                        val id = keyValue.key
                        val enrichedKafkaEvent: EnrichedKafkaEvent = keyValue.value
                        val completeSendToStream = doOperations(enrichedKafkaEvent)
                        if (completeSendToStream) {
                            stateStore.delete(id)
                            context.forward(id, enrichedKafkaEvent)
                            context.commit()
                        } else {
                            stateStore.put(id, enrichedKafkaEvent)
                        }
                    }
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
                } else {
                    stateStore.putIfAbsent(id, enrichedKafkaEvent)
                }
                return keyValue
            }

            private fun doOperations(enrichedKafkaEvent: EnrichedKafkaEvent): Boolean {
                MDC.put(MDCConstants.CORRELATION_ID, enrichedKafkaEvent.correlationId)
                try {
                    manuellOperation.doStuff(enrichedKafkaEvent)
                    return true
                } catch (e: TemporarilyUnavailableException) {
                    enrichedKafkaEvent.incNumFailedAttempts()
                    log.error("Midlertidig nedetid så kunne ikke behandle journalpost ${enrichedKafkaEvent.journalpostId} på forsøk nr. ${enrichedKafkaEvent.numFailedAttempts}", e)
                } catch (e: Exception) {
                    enrichedKafkaEvent.incNumFailedAttempts()
                    log.error("Uventet feil på journalpost ${enrichedKafkaEvent.journalpostId} etter forsøk nr. ${enrichedKafkaEvent.numFailedAttempts}", e)
                    return true
                } finally {
                    MDC.clear()
                }
                return false
            }

            override fun close() {
                // Note: The store should NOT be closed manually here via `stateStore.close()`!
                // The Kafka Streams API will automatically close stores when necessary.
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppgaveOperationsTransformerSupplier::class.java)
    }
}
