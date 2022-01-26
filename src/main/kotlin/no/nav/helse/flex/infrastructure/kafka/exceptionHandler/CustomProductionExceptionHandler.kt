package no.nav.helse.flex.infrastructure.kafka.exceptionHandler

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.streams.errors.ProductionExceptionHandler
import org.apache.kafka.streams.errors.ProductionExceptionHandler.ProductionExceptionHandlerResponse
import org.slf4j.LoggerFactory
import java.lang.Exception

class CustomProductionExceptionHandler : ProductionExceptionHandler {
    private val log = LoggerFactory.getLogger(CustomProductionExceptionHandler::class.java)

    override fun handle(
        producerRecord: ProducerRecord<ByteArray, ByteArray>,
        exception: Exception
    ): ProductionExceptionHandlerResponse {
        log.error(
            "Kafka failed. Message: [${producerRecord.value()}], destination topic: [${producerRecord.topic()}]",
            exception
        )

        if (exception is AuthorizationException) {
            log.error(
                "Caught Authorization Exception in kafka stream, Most likerly cause by rotating credential - try reload environment",
                exception
            )
            return ProductionExceptionHandlerResponse.CONTINUE
        }

        excepction_counter++
        log.error("Kafka have caught exception {} times since pods start", excepction_counter, exception)
        return ProductionExceptionHandlerResponse.CONTINUE
    }

    override fun configure(configs: Map<String?, *>?) {}

    companion object {
        private var excepction_counter = 0
    }
}
