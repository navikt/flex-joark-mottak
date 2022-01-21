package no.nav.helse.flex.infrastructure.kafka;

import no.nav.helse.flex.Environment;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;

public class JfrGenerellKafkaService {
    private static final Logger log = LoggerFactory.getLogger(JfrGenerellKafkaService.class);
    private static int numberOfRestart = 0;

    public void start() throws Exception {
        log.info("Starter jfrKafkaStream");
        try {
            startKafkaStream();
        } catch (final Exception e) {
            log.error("Det oppstod en feil under oppstart av flex-joark-mottak", e);
            throw e;
        }
    }

    public void restartKafkaStream(){
        numberOfRestart++;
        log.info("Restarter jfrKafkaStream, forsøk restart siden podstart {}", numberOfRestart);
        try {
            startKafkaStream();
        } catch (final Exception e) {
            log.error("Det oppstod en feil under restart av flex-joark-mottak", e);
            restartKafkaStream();
        }
    }

    void startKafkaStream() throws Exception{
        log.info("Starter opp Kafka Stream");
        final JfrAivenKafkaConfig aivenKafkaConfig = new JfrAivenKafkaConfig();

        String inputTopic = Environment.getDokumentEventTopic();
        String manuellTopic = Environment.getManuellTopic();
        Properties properties = aivenKafkaConfig.getKafkaProperties();
        final KafkaStreams aivenStream =  new KafkaStreams(
                new JfrTopologies(inputTopic, manuellTopic).getJfrTopologi(), properties);

        aivenStream.setUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());
        aivenStream.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Kafka Stream stopper!");
            aivenStream.close();
        }));
    }

    class CustomUncaughtExceptionHandler implements StreamsUncaughtExceptionHandler {
        @Override
        public StreamThreadExceptionResponse handle(Throwable exception) {
            if(exception instanceof AuthorizationException){
                log.warn("Authorisation failed, most likely because of credential rotation", exception);
                try{
                    TimeUnit.MINUTES.sleep(1); //wait 10 second for certification to update in drive.
                }catch (InterruptedException ie){
                    //do nothing
                }
            }else{
                log.error("Uncaught exception in Kafka Stream!", exception);
            }
            restartKafkaStream();
            return SHUTDOWN_CLIENT;
        }
    }
}
