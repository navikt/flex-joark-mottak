package no.nav.jfr.generell.infrastructure.kafka.exceptionHandler;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.streams.errors.DeserializationExceptionHandler;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomDeserializationExceptionHandler implements DeserializationExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomDeserializationExceptionHandler.class);
    private static int excepction_counter = 0;

    @Override
    public DeserializationHandlerResponse handle(ProcessorContext processorContext, ConsumerRecord<byte[], byte[]> consumerRecord, Exception exception) {
        log.error("Kafka deserialization failed. Message: [{}], destination topic: [{}]",  new String(consumerRecord.value()), consumerRecord.topic(), exception);
        if(exception instanceof AuthorizationException){
            log.error("Caught Authorization Exception in kafka stream, Most likerly cause by rotating credential - try reload environment");
            return DeserializationHandlerResponse.CONTINUE;
        }
        excepction_counter++;
        log.error("Kafka deserialization have caught exception {} times since pods start",  excepction_counter);
        return DeserializationHandlerResponse.CONTINUE;
    }

    @Override
    public void configure(final Map<String, ?> configs) {
    }

}
