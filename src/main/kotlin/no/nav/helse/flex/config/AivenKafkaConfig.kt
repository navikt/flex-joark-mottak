package no.nav.helse.flex.config

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

private const val JAVA_KEYSTORE = "JKS"
private const val PKCS12 = "PKCS12"

@Configuration
class AivenKafkaConfig(
    @Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
    @Value("\${KAFKA_SECURITY_PROTOCOL:SSL}") private val kafkaSecurityProtocol: String,
    @Value("\${KAFKA_TRUSTSTORE_PATH}") private val kafkaTruststorePath: String,
    @Value("\${KAFKA_CREDSTORE_PASSWORD}") private val kafkaCredstorePassword: String,
    @Value("\${KAFKA_KEYSTORE_PATH}") private val kafkaKeystorePath: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY}") private val kafkaSchemaRegistryUrl: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY_USER}") private val schemaRegistryUsername: String,
    @Value("\${KAFKA_SCHEMA_REGISTRY_PASSWORD}") private val schemaRegistryPassword: String,
    @Value("\${aiven-kafka.auto-offset-reset}") private val kafkaAutoOffsetReset: String,
) {
    fun commonConfig() =
        mapOf(
            BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
        ) + securityConfig()

    private fun securityConfig() =
        mapOf(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to kafkaSecurityProtocol,
            // Disables server host name verification.
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to JAVA_KEYSTORE,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to PKCS12,
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to kafkaTruststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to kafkaKeystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to kafkaCredstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to kafkaCredstorePassword,
        )

    @Bean
    fun aivenSchemaRegistryClient(): SchemaRegistryClient =
        CachedSchemaRegistryClient(
            kafkaSchemaRegistryUrl,
            20,
            mapOf(
                KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                KafkaAvroDeserializerConfig.USER_INFO_CONFIG to "$schemaRegistryUsername:$schemaRegistryPassword",
            ),
        )

    @Bean
    fun kafkaAvroListenerContainerFactory(
        aivenSchemaRegistryClient: SchemaRegistryClient,
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, GenericRecord> {
        val genericAvroConsumerConfig =
            commonConfig() +
                mapOf(
                    KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaSchemaRegistryUrl,
                    KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to false,
                    ConsumerConfig.GROUP_ID_CONFIG to "flex-joark-mottak",
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaAutoOffsetReset,
                    ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                    ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to KafkaAvroDeserializer::class.java,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000",
                )

        val consumerFactory =
            DefaultKafkaConsumerFactory(
                genericAvroConsumerConfig,
                StringDeserializer(),
                KafkaAvroDeserializer(aivenSchemaRegistryClient),
            )

        val factory = ConcurrentKafkaListenerContainerFactory<String, GenericRecord>()
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.consumerFactory = consumerFactory
        return factory
    }

    @Bean
    fun aivenKafkaListenerContainerFactory(
        aivenKafkaErrorHandler: AivenKafkaErrorHandler,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val config =
            mapOf(
                ConsumerConfig.GROUP_ID_CONFIG to "flex-joark-mottak-retry",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to kafkaAutoOffsetReset,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "600000",
            ) + commonConfig()
        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(config)

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(aivenKafkaErrorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        return factory
    }

    @Bean
    fun kafkaStringProducer(): KafkaProducer<String, String> {
        val configs =
            mapOf(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 10,
                ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
            ) + commonConfig()
        return KafkaProducer<String, String>(configs)
    }
}
