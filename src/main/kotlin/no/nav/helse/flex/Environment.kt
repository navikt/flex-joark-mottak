package no.nav.helse.flex

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.commons.configuration2.BaseConfiguration
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.EnvironmentConfiguration
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object Environment {
    private val log = LoggerFactory.getLogger(Environment::class.java)

    private val compositeConfiguration = CompositeConfiguration()

    private const val KAFKA_BROKERS	= "KAFKA_BROKERS"
    private const val KAFKA_SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY"
    private const val KAFKA_SCHEMA_REGISTRY_USER = "KAFKA_SCHEMA_REGISTRY_USER"
    private const val KAFKA_SCHEMA_REGISTRY_PASSWORD = "KAFKA_SCHEMA_REGISTRY_PASSWORD"
    private const val KAFKA_STREAMS_APPLICATION_ID = "KAFKA_STREAMS_APPLICATION_ID"
    private const val KAFKA_TRUSTSTORE_PATH = "KAFKA_TRUSTSTORE_PATH"
    private const val KAFKA_CREDSTORE_PASSWORD = "KAFKA_CREDSTORE_PASSWORD"
    private const val KAFKA_KEYSTORE_PATH = "KAFKA_KEYSTORE_PATH"

    private const val AIVEN_DOKUMENT_TOPIC = "AIVEN_DOKUMENT_TOPIC"

    private const val STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING = "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING"

    private const val AZURE_APP_CLIENT_ID = "AZURE_APP_CLIENT_ID"
    private const val AZURE_APP_CLIENT_SECRET = "AZURE_APP_CLIENT_SECRET"
    private const val AZURE_APP_WELL_KNOWN_URL = "AZURE_APP_WELL_KNOWN_URL"
    private const val SAF_CLIENT_ID = "SAF_CLIENT_ID"
    private const val SAF_URL = "SAF_URL"
    private const val DOKARKIV_CLIENT_ID = "DOKARKIV_CLIENT_ID"
    private const val JOURNALPOSTAPI_URL = "JOURNALPOSTAPI_URL"
    private const val PDL_CLIENT_ID = "PDL_CLIENT_ID"
    private const val PDL_URL = "PDL_URL"
    private const val OPPGAVE_CLIENT_ID = "OPPGAVE_CLIENT_ID"
    private const val OPPGAVE_URL = "OPPGAVE_URL"
    private const val FKV_URL = "FKV_URL"
    private const val FLEX_FSS_PROXY_CLIENT_ID = "FLEX_FSS_PROXY_CLIENT_ID"

    init {
        compositeConfiguration.addConfiguration(EnvironmentConfiguration())
        log.info("Konfigurasjon lastet fra system- og miljøvariabler")
        try {
            val baseConfig: Configuration = BaseConfiguration()
            baseConfig.addProperty(
                "AZURE_APP_CLIENT_ID",
                getPropertyValueFromVault("/var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_ID")
            )
            baseConfig.addProperty(
                "AZURE_APP_CLIENT_SECRET",
                getPropertyValueFromVault("/var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_SECRET")
            )
            compositeConfiguration.addConfiguration(baseConfig)
        } catch (e: Exception) {
            log.error("Vault setup failed: $e")
        }
    }

    val bootstrapServersUrl get() = getEnvVar(KAFKA_BROKERS)
    val kafkaSchemaRegistryUrl get() = getEnvVar(KAFKA_SCHEMA_REGISTRY)
    val kafkaUserInfoConfig get() = "${getEnvVar(KAFKA_SCHEMA_REGISTRY_USER)}:${
    getEnvVar(
        KAFKA_SCHEMA_REGISTRY_PASSWORD
    )
    }"
    val applicationId get() = getEnvVar(KAFKA_STREAMS_APPLICATION_ID)
    val kafkaTruststorePath get() = getEnvVar(KAFKA_TRUSTSTORE_PATH)
    val kafkaCredstorePassword get() = getEnvVar(KAFKA_CREDSTORE_PASSWORD)
    val kafkaKeystorePath get() = getEnvVar(KAFKA_KEYSTORE_PATH)
    val kafkaSerdeConfig get() = mapOf(
        AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to kafkaSchemaRegistryUrl,
        SchemaRegistryClientConfig.USER_INFO_CONFIG to kafkaUserInfoConfig,
        SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
    )

    val dokumentEventTopic get() = getEnvVar(AIVEN_DOKUMENT_TOPIC)

    fun getSkjemaerJson() =
        readSupportedTemaerOgSkjemaerFromFile(getEnvVar(STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING))

    val azureClientId get() = getEnvVar(AZURE_APP_CLIENT_ID)
    val azureAppClientSecret get() = getEnvVar(AZURE_APP_CLIENT_SECRET)
    val azureAppURL get() = getEnvVar(AZURE_APP_WELL_KNOWN_URL)

    val oppgaveClientId get() = getEnvVar(OPPGAVE_CLIENT_ID)
    val oppgaveUrl get() = getEnvVar(OPPGAVE_URL)

    val safClientId get() = getEnvVar(SAF_CLIENT_ID)
    val safUrl get() = getEnvVar(SAF_URL)

    val proxyClientid get() = getEnvVar(FLEX_FSS_PROXY_CLIENT_ID)
    val fkvUrl get() = getEnvVar(FKV_URL)

    val pdlClientid get() = getEnvVar(PDL_CLIENT_ID)
    val persondataUrl get() = getEnvVar(PDL_URL)

    val dokarkivClientId get() = getEnvVar(DOKARKIV_CLIENT_ID)
    val journalpostApiUrl get() = getEnvVar(JOURNALPOSTAPI_URL)

    fun getEnvVar(varName: String): String {
        val envVar = compositeConfiguration.getString(varName)
        if (envVar == null || envVar.isEmpty()) {
            log.warn("Missing environment variable for $varName and default value is null")
            throw IllegalArgumentException("Missing environment variable for $varName and default value is null")
        }
        return envVar
    }

    private fun getPropertyValueFromVault(path: String): String? {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            log.error("Klarte ikke laste property for path {}", path)
        }
        return null
    }

    private fun readFile(reader: BufferedReader): String {
        val stringBuilder = StringBuilder()
        while (reader.ready()) {
            stringBuilder.append(reader.readLine().trim { it <= ' ' })
        }
        return stringBuilder.toString()
    }

    private fun readSupportedTemaerOgSkjemaerFromFile(fileName: String): String {
        val s = Environment::class.java.getResourceAsStream("/$fileName")!!
        val reader = BufferedReader(InputStreamReader(s))
        val tilganger: String
        try {
            tilganger = readFile(reader)
            reader.close()
        } catch (e: IOException) {
            log.error("Feil i lesing av støttede temaer og skjemaer: {}", e.message)
            throw UncheckedIOException(e)
        }
        return tilganger
    }
}
