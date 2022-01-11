package no.nav.jfr.generell.infrastructure.kafka.exceptionHandler;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.streams.errors.ProductionExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomProductionExceptionHandler implements ProductionExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomProductionExceptionHandler.class);
    private static int excepction_counter = 0;

    @Override
    public ProductionExceptionHandlerResponse handle(ProducerRecord<byte[], byte[]> producerRecord, Exception exception) {
        log.error("Kafka failed. Message: [{}], destination topic: [{}]",  new String(producerRecord.value()), producerRecord.topic(), exception);
        if(exception instanceof AuthorizationException){
            log.error("Caught Authorization Exception in kafka stream, Most likerly cause by rotating credential - try reload environment", exception);
            return ProductionExceptionHandlerResponse.CONTINUE;
        }
        excepction_counter++;
        log.error("Kafka have caught exception {} times since pods start",  excepction_counter, exception);
        return ProductionExceptionHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }

}
