package no.nav.helse.flex.infrastructure.kafka

import no.nav.helse.flex.Environment.dokumentEventTopic
import org.apache.kafka.common.errors.AuthorizationException
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class JfrGenerellKafkaService {
    fun start() {
        log.info("Starter jfrKafkaStream")
        try {
            startKafkaStream()
        } catch (e: Exception) {
            log.error("Det oppstod en feil under oppstart av flex-joark-mottak", e)
            throw e
        }
    }

    fun restartKafkaStream() {
        numberOfRestart++
        log.info("Restarter jfrKafkaStream, forsøk restart siden podstart $numberOfRestart")
        try {
            startKafkaStream()
        } catch (e: Exception) {
            log.error("Det oppstod en feil under restart av flex-joark-mottak", e)
            restartKafkaStream()
        }
    }

    fun startKafkaStream() {
        log.info("Starter opp Kafka Stream")
        val aivenKafkaConfig = JfrAivenKafkaConfig()
        val inputTopic = dokumentEventTopic
        val properties = aivenKafkaConfig.kafkaProperties
        val aivenStream = KafkaStreams(JfrTopologies(inputTopic).jfrTopologi, properties)
        aivenStream.setUncaughtExceptionHandler(CustomUncaughtExceptionHandler())
        // aivenStream.start()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Kafka Stream stopper!")
                aivenStream.close()
            }
        )
    }

    internal inner class CustomUncaughtExceptionHandler : StreamsUncaughtExceptionHandler {
        override fun handle(exception: Throwable): StreamThreadExceptionResponse {
            if (exception is AuthorizationException) {
                log.warn("Authorisation failed, most likely because of credential rotation", exception)
                try {
                    TimeUnit.MINUTES.sleep(1) // wait 10 second for certification to update in drive.
                } catch (ie: InterruptedException) {
                    // do nothing
                }
            } else {
                log.error("Uncaught exception in Kafka Stream!", exception)
            }
            restartKafkaStream()
            return StreamThreadExceptionResponse.SHUTDOWN_CLIENT
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(JfrGenerellKafkaService::class.java)
        private var numberOfRestart = 0
    }
}
