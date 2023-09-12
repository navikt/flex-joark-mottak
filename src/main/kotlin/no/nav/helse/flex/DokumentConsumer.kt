package no.nav.helse.flex

import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class DokumentConsumer {
    val log = logger()

    @KafkaListener(
        topics = ["#{environmentToggles.dokumentTopic()}"],
        id = "flex-joark-mottak",
        idIsGroup = true,
        containerFactory = "kafkaAvroListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"]
    )
    fun listen(cr: ConsumerRecord<String, GenericRecord>, acknowledgment: Acknowledgment) {
        log.info("Key: ${cr.key()}, Value: ${cr.value()}")
        acknowledgment.acknowledge()
    }
}
