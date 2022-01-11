package no.nav.helse.flex;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Environment {
    private static final Logger log = LoggerFactory.getLogger(Environment.class);
    private static final CompositeConfiguration compositeConfiguration = new CompositeConfiguration();
    private static final String KAFKA_BOOTSTRAP_SERVERS = "KAFKA_BOOTSTRAP_SERVERS_URL";
    private static final String KAFKA_SCHEMA_REGISTRY = "KAFKA_SCHEMA_REGISTRY";
    private static final String KAFKA_SCHEMA_REGISTRY_USER = "KAFKA_SCHEMA_REGISTRY_USER";
    private static final String KAFKA_SCHEMA_REGISTRY_PASSWORD = "KAFKA_SCHEMA_REGISTRY_PASSWORD";
    private static final String KAFKA_STREAMS_APPLICATION_ID = "KAFKA_STREAMS_APPLICATION_ID";
    private static final String AIVEN_DOKUMENT_TOPIC = "AIVEN_DOKUMENT_TOPIC";
    private static final String AIVEN_TO_MANUELL_TOPIC = "AIVEN_TO_MANUELL_TOPIC";
    private static final String STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING = "STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING";
    private static final String AZURE_APP_CLIENT_ID = "AZURE_APP_CLIENT_ID";
    private static final String AZURE_APP_CLIENT_SECRET = "AZURE_APP_CLIENT_SECRET";
    private static final String AZURE_APP_WELL_KNOWN_URL = "AZURE_APP_WELL_KNOWN_URL";
    private static final String NAIS_CLUSTER_NAME = "NAIS_CLUSTER_NAME";

    private static final String SAF_CLIENT_ID = "SAF_CLIENT_ID";
    private static final String SAF_URL = "SAF_URL";
    private static final String DOKARKIV_CLIENT_ID = "DOKARKIV_CLIENT_ID";
    private static final String JOURNALPOSTAPI_URL = "JOURNALPOSTAPI_URL";
    private static final String PDL_CLIENT_ID = "PDL_CLIENT_ID";
    private static final String PDL_URL = "PDL_URL";
    private final static String OPPGAVE_CLIENT_ID = "OPPGAVE_CLIENT_ID";
    private static final String OPPGAVE_URL = "OPPGAVE_URL";
    private static final String FKV_URL = "FKV_URL";
    private static final String NORG2_URL = "NORG2_URL";


    static {
        setupConfigFile();
    }

    public static String getDokumentEventTopic(){
        return getEnvVar(AIVEN_DOKUMENT_TOPIC);
    }

    public static String getManuellTopic(){
        return getEnvVar(AIVEN_TO_MANUELL_TOPIC);
    }

    public static String getEnvVar(final String varName) throws IllegalArgumentException {
        if(compositeConfiguration==null) return "";
        final String envVar = compositeConfiguration.getString(varName);
        if (envVar == null || envVar.isEmpty()) {
            log.warn("Missing environment variable for " + varName + " and default value is null");
            throw new IllegalArgumentException("Missing environment variable for " + varName + " and default value is null");
        }
        return envVar;
    }

    public static String getBootstrapServersUrl() {
        return getEnvVar(KAFKA_BOOTSTRAP_SERVERS);
    }

    private static void setupConfigFile() {
        compositeConfiguration.addConfiguration(new EnvironmentConfiguration());
        log.info("Konfigurasjon lastet fra system- og miljøvariabler");
        try {
            Configuration baseConfig = new BaseConfiguration();
            baseConfig.addProperty("AZURE_APP_CLIENT_ID", getPropertyValueFromVault("/var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_ID"));
            baseConfig.addProperty("AZURE_APP_CLIENT_SECRET", getPropertyValueFromVault("/var/run/secrets/nais.io/azure/AZURE_APP_CLIENT_SECRET"));

            compositeConfiguration.addConfiguration(baseConfig);
        } catch (Exception e) {
            log.error("Vault setup failed: " + e);
        }
    }

    private static String getPropertyValueFromVault(String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Klarte ikke laste property for path {}", path);
        }
        return null;
    }

    public static boolean erProd() {
        return getEnvVar(NAIS_CLUSTER_NAME).equals("prod-gcp");
    }

    public static String getApplicationId() {
        return  getEnvVar(KAFKA_STREAMS_APPLICATION_ID);
    }

    public static String getSafUrl() {
        return getEnvVar(SAF_URL);
    }

    public static String azureClientId() {
        return getEnvVar(AZURE_APP_CLIENT_ID);
    }

    public static String getOppgaveClientId() {
        return getEnvVar(OPPGAVE_CLIENT_ID);
    }

    public static String getPdlClientid() {
        return getEnvVar(PDL_CLIENT_ID);
    }

    public static String getOppgaveUrl() {
        return getEnvVar(OPPGAVE_URL);
    }

    public static String getFkvUrl() {
        return getEnvVar(FKV_URL);
    }

    public static String azureAppClientSecret() {
        return getEnvVar(AZURE_APP_CLIENT_SECRET);
    }

    public static String azureAppURL() {
        return getEnvVar(AZURE_APP_WELL_KNOWN_URL);
    }

    public static String getSafClientId() {
        return getEnvVar(SAF_CLIENT_ID);
    }

    public static String getPersondataUrl() {
        return getEnvVar(PDL_URL);
    }

    public static String getNorg2Url(){
        return getEnvVar(NORG2_URL);
    }

    public static String getJournalpostApiUrl() { return getEnvVar(JOURNALPOSTAPI_URL); }

    public static String getDokarkivClientId() { return getEnvVar(DOKARKIV_CLIENT_ID); }

    //Understands valid formats of username and password strings and objects
//    private static class Credentials {
//        static String kafkaSaslAuthentication(final String username, final String password) {
//            return String.format("username=%s password=%s", username, password);
//        }
//
//        static UsernamePasswordCredentials stsOicdCredentials(final String username, final String password) {
//            return new UsernamePasswordCredentials(username, password);
//        }
//    }

    public static String getSkjemaerJson() {
        return readSupportedTemaerOgSkjemaerFromFile(getEnvVar(STOTTEDE_TEMAER_OG_SKJEMAER_FILPLASSERING));
    }

    private static String readFile(final BufferedReader reader) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        while (reader.ready()) {
            stringBuilder.append(reader.readLine().trim());
        }
        return stringBuilder.toString();
    }

    private static String readSupportedTemaerOgSkjemaerFromFile(final String fileName) {
        final InputStream s = Environment.class.getResourceAsStream("/" + fileName);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(s));
        String tilganger;
        try{
            tilganger = readFile(reader);
            reader.close();
        } catch (IOException e){
            log.error("Feil i lesing av støttede temaer og skjemaer: {}", e.getMessage());
            throw new UncheckedIOException(e);
        }
        return tilganger;
    }

    public static String getKafkaSchemaRegistryUrl(){
        return Environment.getEnvVar(KAFKA_SCHEMA_REGISTRY);
    }

    public static String getKafkaUserInfoConfig() {
        return Environment.getEnvVar(KAFKA_SCHEMA_REGISTRY_USER)
                + ":" + Environment.getEnvVar(KAFKA_SCHEMA_REGISTRY_PASSWORD);
    }

    public static Map<String, String> getKafkaSerdeConfig() {
        return Map.of(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, getKafkaSchemaRegistryUrl(),
                SchemaRegistryClientConfig.USER_INFO_CONFIG, Environment.getKafkaUserInfoConfig(),
                SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
    }
}
