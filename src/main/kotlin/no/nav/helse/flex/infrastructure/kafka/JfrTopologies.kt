package no.nav.helse.flex.infrastructure.kafka

import com.google.gson.Gson
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde
import no.nav.helse.flex.Environment.kafkaSerdeConfig
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.EventEnricherTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.GenerellOperationsTransformerSupplier
import no.nav.helse.flex.infrastructure.kafka.transformerSupplier.JournalOperationsTransformerSupplier
import no.nav.helse.flex.infrastructure.metrics.Metrics.incJfrAutoProcess
import no.nav.helse.flex.infrastructure.metrics.Metrics.incJfrManuallProcess
import no.nav.helse.flex.operations.Feilregistrer
import no.nav.helse.flex.operations.SkjemaMetadata
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.Stores
import org.slf4j.LoggerFactory
import org.slf4j.MDC

class JfrTopologies(
    private val inputTopic: String,
    private val manuellTopic: String,
    private val skjemaMetadata: SkjemaMetadata = SkjemaMetadata(),
    private val feilregistrer: Feilregistrer = Feilregistrer(),
    private val eventEnricherTransformerSupplier: EventEnricherTransformerSupplier = EventEnricherTransformerSupplier(ENRICHER_OPERATION_STORE),
    private val generellOperationsTransformerSupplier: GenerellOperationsTransformerSupplier = GenerellOperationsTransformerSupplier(GENERELL_OPERATION_STORE),
    private val journalOperationsTransformerSupplier: JournalOperationsTransformerSupplier = JournalOperationsTransformerSupplier(JOURNALFOERING_OPERATION_STORE)
) {

    private val enhancedKafkaEventSerde = Serdes.serdeFrom(
        JfrKafkaSerializer(),
        JfrKafkaDeserializer(
            EnrichedKafkaEvent::class.java
        )
    )
    private val eventEnricherSupplier = Stores.keyValueStoreBuilder(
        Stores.inMemoryKeyValueStore(ENRICHER_OPERATION_STORE),
        Serdes.String(),
        enhancedKafkaEventSerde
    )
    private val generellOperationSupplier = Stores.keyValueStoreBuilder(
        Stores.inMemoryKeyValueStore(GENERELL_OPERATION_STORE),
        Serdes.String(),
        enhancedKafkaEventSerde
    )
    private val journalOperationSupplier = Stores.keyValueStoreBuilder(
        Stores.inMemoryKeyValueStore(JOURNALFOERING_OPERATION_STORE),
        Serdes.String(),
        enhancedKafkaEventSerde
    )

    val jfrTopologi: Topology
        get() {
            val streamsBuilder = StreamsBuilder()
            val serdeConfig: Map<String, String?> = kafkaSerdeConfig
            val valueGenericAvroSerde: Serde<GenericRecord> = GenericAvroSerde()
            valueGenericAvroSerde.configure(serdeConfig, false) // `false` for record values
            streamsBuilder.addStateStore(eventEnricherSupplier)
            streamsBuilder.addStateStore(generellOperationSupplier)
            streamsBuilder.addStateStore(journalOperationSupplier)

            val aapenDokStream = streamsBuilder.stream(
                inputTopic, Consumed.with(Serdes.String(), valueGenericAvroSerde)
            )
            val filteredEvents = filterGenerelleEvent(aapenDokStream)
            val kafkaEventKStream = convertGenericRecordToKafkaEvent(filteredEvents)
            val enrichedEventsStream = enrichKafkaEvent(kafkaEventKStream)
            val filterJournalpostToAuto = filterJournalpostToAuto(enrichedEventsStream)
            val processedGenerellStream = generellOperations(filterJournalpostToAuto)
            journalfoerJournalpost(processedGenerellStream)

            return streamsBuilder.build()
        }

    private fun filterGenerelleEvent(inputStream: KStream<String, GenericRecord>): KStream<String, GenericRecord> {
        return inputStream
            .filter { _, genericRecord ->
                genericRecord["temaNytt"].toString() == "SYK" && eventType.contains(
                    genericRecord["hendelsesType"].toString()
                )
            }
    }

    private fun convertGenericRecordToKafkaEvent(genericRecordString: KStream<String, GenericRecord>): KStream<String, KafkaEvent> {
        return genericRecordString.mapValues { genericRecord: GenericRecord ->
            Gson().fromJson(
                genericRecord.toString(),
                KafkaEvent::class.java
            )
        }
    }

    private fun enrichKafkaEvent(kafkaEventKStream: KStream<String, KafkaEvent>): KStream<String, EnrichedKafkaEvent> {
        val enrichedKafkaEvent = kafkaEventKStream.transform(eventEnricherTransformerSupplier, ENRICHER_OPERATION_STORE)
        val toManuell =
            enrichedKafkaEvent.filter { _, jfrEnhancedKafkaEvent -> jfrEnhancedKafkaEvent.isToManuell && !jfrEnhancedKafkaEvent.isToIgnore }
        sendToJfrManuellOppretter(toManuell)
        return enrichedKafkaEvent.filter { _, jfrEnhancedKafkaEvent -> !jfrEnhancedKafkaEvent.isToManuell && !jfrEnhancedKafkaEvent.isToIgnore }
    }

    private fun filterJournalpostToAuto(enrichedKafkaEventStream: KStream<String, EnrichedKafkaEvent>): KStream<String, EnrichedKafkaEvent> {
        val toAuto = enrichedKafkaEventStream.filter { _, jfrEnhancedKafkaEvent ->
            skjemaMetadata.inAutoList(
                jfrEnhancedKafkaEvent.tema,
                jfrEnhancedKafkaEvent.skjema
            )
        }
            .peek { _, enrichedKafkaEvent ->
                logWithCorrelationId(
                    enrichedKafkaEvent, "Forsøker automatisk behandling på journalpost ${enrichedKafkaEvent.journalpostId} med tema ${enrichedKafkaEvent.tema} og skjema ${enrichedKafkaEvent.skjema}"
                )
            }
        val toManuell = enrichedKafkaEventStream.filterNot { _, jfrEnhancedKafkaEvent ->
            skjemaMetadata.inAutoList(
                jfrEnhancedKafkaEvent.tema,
                jfrEnhancedKafkaEvent.skjema
            )
        }
        sendToJfrManuellOppretter(toManuell)
        return toAuto
    }

    private fun generellOperations(enrichedKafkaEventStream: KStream<String, EnrichedKafkaEvent>): KStream<String, EnrichedKafkaEvent> {
        val afterGenerellOperationStream =
            enrichedKafkaEventStream.transform(generellOperationsTransformerSupplier, GENERELL_OPERATION_STORE)
        val toManuell =
            afterGenerellOperationStream.filter { _, enrichedKafkaEvent -> enrichedKafkaEvent.isToManuell }
        sendToJfrManuellOppretter(toManuell)
        return afterGenerellOperationStream.filterNot { _, enrichedKafkaEvent -> enrichedKafkaEvent.isToManuell }
    }

    private fun journalfoerJournalpost(oppgaveFilteredStream: KStream<String, EnrichedKafkaEvent>) {
        val journalpostAfterJournalfoering =
            oppgaveFilteredStream.transform(journalOperationsTransformerSupplier, JOURNALFOERING_OPERATION_STORE)
        val toManuell =
            journalpostAfterJournalfoering.filter { _, enrichedKafkaEvent -> enrichedKafkaEvent.isToManuell }
        val doneProcessing =
            journalpostAfterJournalfoering.filterNot { _, enrichedKafkaEvent -> enrichedKafkaEvent.isToManuell }
        doneProcessing.peek { _, enrichedKafkaEvent ->
            incJfrAutoProcess(
                enrichedKafkaEvent!!
            )
        }
        sendToJfrManuellOppretter(toManuell)
    }

    private fun sendToJfrManuellOppretter(manuelle: KStream<String, EnrichedKafkaEvent>) {
        manuelle.foreach { _, enrichedKafkaEvent1 ->
            feilregistrer.feilregistrerOppgave(
                enrichedKafkaEvent1!!
            )
        }
        manuelle.peek { _, enrichedKafkaEvent ->
            incJfrManuallProcess(
                enrichedKafkaEvent,
                skjemaMetadata.inAutoList(enrichedKafkaEvent.tema, enrichedKafkaEvent.skjema)
            )
        }
        manuelle.peek { _, enrichKafkaEvent ->
            logWithCorrelationId(
                enrichKafkaEvent,
                "Journalposten: ${enrichKafkaEvent.journalpostId} sendes til manuell-oppretter"
            )
        }
            .to(manuellTopic, Produced.with(Serdes.String(), enhancedKafkaEventSerde))
    }

    private fun logWithCorrelationId(enrichedKafkaEvent: EnrichedKafkaEvent, s: String) {
        MDC.put("CORRELATION_ID", enrichedKafkaEvent.correlationId)
        log.info(s)
        MDC.clear()
    }

    companion object {
        private val log = LoggerFactory.getLogger(JfrTopologies::class.java)
        private val eventType = listOf("MidlertidigJournalført", "Mottatt", "JournalpostMottatt")
        private const val ENRICHER_OPERATION_STORE = "enrichoperations"
        private const val GENERELL_OPERATION_STORE = "generelloperations"
        private const val JOURNALFOERING_OPERATION_STORE = "journalfoeringoperations"
    }
}
