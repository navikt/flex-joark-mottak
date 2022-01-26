package no.nav.helse.flex.infrastructure.kafka

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import no.nav.helse.flex.Environment.applicationId
import no.nav.helse.flex.Environment.bootstrapServersUrl
import no.nav.helse.flex.Environment.kafkaCredstorePassword
import no.nav.helse.flex.Environment.kafkaKeystorePath
import no.nav.helse.flex.Environment.kafkaSchemaRegistryUrl
import no.nav.helse.flex.Environment.kafkaTruststorePath
import no.nav.helse.flex.Environment.kafkaUserInfoConfig
import no.nav.helse.flex.infrastructure.kafka.exceptionHandler.CustomDeserializationExceptionHandler
import no.nav.helse.flex.infrastructure.kafka.exceptionHandler.CustomProductionExceptionHandler
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import java.util.HashMap
import java.util.Properties

class JfrAivenKafkaConfig internal constructor() {
    val kafkaProperties: Properties

    init {
        log.info("Kafka Stream Properties")
        kafkaProperties = Properties()
        kafkaProperties[CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG] = 5000
        kafkaProperties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"
        kafkaProperties[StreamsConfig.NUM_STREAM_THREADS_CONFIG] = 1
        kafkaProperties[StreamsConfig.APPLICATION_ID_CONFIG] = applicationId
        kafkaProperties[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServersUrl
        kafkaProperties[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
        kafkaProperties[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
        kafkaProperties[StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            CustomDeserializationExceptionHandler::class.java
        kafkaProperties[StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            CustomProductionExceptionHandler::class.java
        kafkaProperties[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] =
            kafkaSchemaRegistryUrl
        kafkaProperties[SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = USER_INFO
        kafkaProperties[SchemaRegistryClientConfig.USER_INFO_CONFIG] = kafkaUserInfoConfig
        kafkaProperties.putAll(aivenKafkaConfig())
    }

    private fun aivenKafkaConfig(): Map<String?, String?> {
        val aivenKafkaConfigs: MutableMap<String?, String?> = HashMap()
        aivenKafkaConfigs[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        aivenKafkaConfigs[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServersUrl
        aivenKafkaConfigs[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
        aivenKafkaConfigs[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = JAVA_KEYSTORE
        aivenKafkaConfigs[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = PKCS12
        aivenKafkaConfigs[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = kafkaTruststorePath
        aivenKafkaConfigs[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = kafkaCredstorePassword
        aivenKafkaConfigs[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = kafkaKeystorePath
        aivenKafkaConfigs[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = kafkaCredstorePassword
        aivenKafkaConfigs[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = kafkaCredstorePassword
        return aivenKafkaConfigs
    }

    companion object {
        private val log = LoggerFactory.getLogger(JfrAivenKafkaConfig::class.java)
        private const val USER_INFO = "USER_INFO"
        private const val JAVA_KEYSTORE = "JKS"
        private const val PKCS12 = "PKCS12"
    }
}
