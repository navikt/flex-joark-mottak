package no.nav.helse.flex.infrastructure.kafka.exceptionHandler

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.streams.errors.DeserializationExceptionHandler
import org.apache.kafka.streams.errors.DeserializationExceptionHandler.DeserializationHandlerResponse
import org.apache.kafka.streams.processor.ProcessorContext
import org.slf4j.LoggerFactory
import java.lang.Exception

class CustomDeserializationExceptionHandler : DeserializationExceptionHandler {
    private val log = LoggerFactory.getLogger(CustomDeserializationExceptionHandler::class.java)

    override fun handle(
        processorContext: ProcessorContext,
        consumerRecord: ConsumerRecord<ByteArray, ByteArray>,
        exception: Exception
    ): DeserializationHandlerResponse {
        log.error(
            "Kafka deserialization failed. Message: [${consumerRecord.value()}], destination topic: [${consumerRecord.topic()}]",
            exception
        )

        if (exception is AuthorizationException) {
            log.error("Caught Authorization Exception in kafka stream, Most likerly cause by rotating credential - try reload environment")
            return DeserializationHandlerResponse.CONTINUE
        }

        excepction_counter++
        log.error("Kafka deserialization have caught exception {} times since pods start", excepction_counter)
        return DeserializationHandlerResponse.CONTINUE
    }

    override fun configure(configs: Map<String?, *>?) {}

    companion object {
        private var excepction_counter = 0
    }
}
