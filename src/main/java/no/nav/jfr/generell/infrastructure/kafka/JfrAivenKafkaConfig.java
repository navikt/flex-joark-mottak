package no.nav.jfr.generell.infrastructure.kafka;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.kafka.exceptionHandler.CustomDeserializationExceptionHandler;
import no.nav.jfr.generell.infrastructure.kafka.exceptionHandler.CustomProductionExceptionHandler;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JfrAivenKafkaConfig {
    private final static Logger log = LoggerFactory.getLogger(JfrAivenKafkaConfig.class);
    private static final String KAFKA_BROKERS = "KAFKA_BROKERS";
    private static final String KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH";
    private static final String KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD";
    private static final String KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH";
    private static final String KAFKA_SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY";
    private static final String USER_INFO = "USER_INFO";
    private static final String JAVA_KEYSTORE = "JKS";
    private static final String PKCS12 = "PKCS12";

    private final Properties kafkaProperties;

    JfrAivenKafkaConfig() {
        log.info("Kafka Stream Properties");
        kafkaProperties = new Properties();
        kafkaProperties.put(CommonClientConfigs.RECONNECT_BACKOFF_MS_CONFIG, 5000);
        kafkaProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaProperties.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1);
        kafkaProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, Environment.getApplicationId());
        kafkaProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, Environment.getBootstrapServersUrl());
        kafkaProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        kafkaProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        kafkaProperties.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, CustomDeserializationExceptionHandler.class);
        kafkaProperties.put(StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG, CustomProductionExceptionHandler.class);
        kafkaProperties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, Environment.getEnvVar(KAFKA_SCHEMA_REGISTRY));
        kafkaProperties.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, USER_INFO);
        kafkaProperties.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, Environment.getKafkaUserInfoConfig());

        kafkaProperties.putAll(aivenKafkaConfig());
    }

    private Map<String, String> aivenKafkaConfig() {
        final Map<String, String> aivenKafkaConfigs = new HashMap<>();
        aivenKafkaConfigs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        aivenKafkaConfigs.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, Environment.getEnvVar(KAFKA_BROKERS));
        aivenKafkaConfigs.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        aivenKafkaConfigs.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, JAVA_KEYSTORE);
        aivenKafkaConfigs.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, PKCS12);
        aivenKafkaConfigs.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, Environment.getEnvVar(KAFKA_TRUSTSTORE_PATH));
        aivenKafkaConfigs.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, Environment.getEnvVar(KAFKA_CREDSTORE_PASSWORD));
        aivenKafkaConfigs.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, Environment.getEnvVar(KAFKA_KEYSTORE_PATH));
        aivenKafkaConfigs.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, Environment.getEnvVar(KAFKA_CREDSTORE_PASSWORD));
        aivenKafkaConfigs.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, Environment.getEnvVar(KAFKA_CREDSTORE_PASSWORD));
        return aivenKafkaConfigs;
    }

    public Properties getKafkaProperties() {
        return kafkaProperties;
    }
}
