package config

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class KafkaConfig(
    @Value("\${KAFKA_BROKERS}") private val kafkaBrokers: String,
) {
    @Bean
    @Primary
    fun aivenSchemaRegistryClient(): SchemaRegistryClient {
        return MockSchemaRegistryClient()
    }

    @Bean
    fun kafkaAvroSerializer(aivenSchemaRegistryClient: SchemaRegistryClient): KafkaAvroSerializer {
        return KafkaAvroSerializer(
            aivenSchemaRegistryClient,
            mapOf(
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://localhost.nav",
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to true,
            ),
        )
    }

    @Bean
    fun kafkaProducer(kafkaAvroSerializer: KafkaAvroSerializer): KafkaProducer<String, Any> {
        val configs =
            mapOf(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to KafkaAvroSerializer::class.java,
                ProducerConfig.ACKS_CONFIG to "all",
                ProducerConfig.RETRIES_CONFIG to 10,
                ProducerConfig.RETRY_BACKOFF_MS_CONFIG to 100,
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaBrokers,
                SaslConfigs.SASL_MECHANISM to "PLAIN",
            )

        return KafkaProducer(
            configs,
            StringSerializer(),
            kafkaAvroSerializer,
        )
    }
}
