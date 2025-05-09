package no.nav.helse.flex.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.util.backoff.ExponentialBackOff

@Component
class AivenKafkaErrorHandler :
    DefaultErrorHandler(
        null,
        ExponentialBackOff(1000L, 1.5).also {
            // 8 minutter, som er mindre enn max.poll.interval.ms på 10 minutter.
            it.maxInterval = 60_000L * 8
        },
    ) {
    private val log = this.logger()

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
    ) {
        records.forEach { record ->
            log.error(
                thrownException,
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic: ${record.topic()}.",
            )
        }
        if (records.isEmpty()) {
            log.error(thrownException, "Feil i listener uten noen records.")
        }

        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleBatch(
        thrownException: Exception,
        data: ConsumerRecords<*, *>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        invokeListener: Runnable,
    ) {
        data.forEach { record ->
            log.error(
                thrownException,
                "Feil i prossesseringen av record med offset: ${record.offset()}, key: ${record.key()} på topic: ${record.topic()}.",
            )
        }
        if (data.isEmpty) {
            log.error(thrownException, "Feil i listener uten noen records.")
        }
        super.handleBatch(thrownException, data, consumer, container, invokeListener)
    }
}
