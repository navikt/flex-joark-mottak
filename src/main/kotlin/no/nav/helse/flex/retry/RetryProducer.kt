package no.nav.helse.flex.retry

import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.logger
import no.nav.helse.flex.serialisertTilString
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class RetryProducer(
    private val kafkaStringProducer: KafkaProducer<String, String>,
) {
    val log = logger()

    fun send(
        kafkaEvent: KafkaEvent,
        behandlingstidspunkt: OffsetDateTime,
    ) {
        kafkaStringProducer
            .send(
                ProducerRecord(
                    RETRY_TOPIC,
                    null,
                    kafkaEvent.journalpostId,
                    kafkaEvent.serialisertTilString(),
                    listOf(
                        RecordHeader(
                            BEHANDLINGSTIDSPUNKT,
                            behandlingstidspunkt
                                .toInstant()
                                .toEpochMilli()
                                .toString()
                                .toByteArray(),
                        ),
                    ),
                ),
            ).get()
    }
}
