package no.nav.jfr.generell.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.jfr.generell.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.substringBetween;

public class AzureAdClient {
    private static final Logger log = LoggerFactory.getLogger(AzureAdClient.class);
    private static final String GRANT_TYPE = "client_credentials";
    private static String discoveryUrl;
    private String token;
    private String clientId;

    public AzureAdClient(String clientId){
        this.clientId = clientId;
        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(Environment.azureAppURL()))
                    .GET()
                    .build();
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            try {
                final HashMap<String, Object> result = new ObjectMapper().readValue(response.body(), HashMap.class);
                discoveryUrl = (String) result.get("token_endpoint");
            } catch (final IOException e) {
                throw new IllegalStateException("Klarte ikke deserialisere respons fra AzureAd", e);
            }
        } catch (final Exception e){
            log.error("Exception under static INIT av AAD client");
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
        this.token = getTokenFromAzureAd();
    }

    public String getToken(){
        if (isExpired(this.token)) {
            token = getTokenFromAzureAd();
        }
        return "Bearer " + token;
    }

    private static boolean isExpired(final String AADToken) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String tokenBody = new String(java.util.Base64.getDecoder().decode(substringBetween(AADToken, ".")));
            final JsonNode node = mapper.readTree(tokenBody);
            final Instant now = Instant.now();
            final Instant expiry = Instant.ofEpochSecond(node.get("exp").longValue()).minusSeconds(300);
            if (now.isAfter(expiry)) {
                log.debug("AzureAd token expired. {} is after {}", now, expiry);
                return true;
            }
        } catch (final IOException e) {
            log.error("Klarte ikke parse token fra AzureAd");
            throw new IllegalStateException("Klarte ikke parse token fra AzureAd", e);
        }
        return false;
    }

    private String getTokenFromAzureAd(){
        try {
            final Map<String, String> parameters = new HashMap<>();
            parameters.put("scope", "api://" + this.clientId + "/.default");
            parameters.put("client_id", Environment.azureClientId());
            parameters.put("client_secret", Environment.azureAppClientSecret());
            parameters.put("grant_type", GRANT_TYPE);
            final String form = parameters.keySet().stream()
                    .map(key -> key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(discoveryUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            final AzureAdResponse azureAdResponse;
            try {
                azureAdResponse = new ObjectMapper().readValue(response.body(), AzureAdResponse.class);
                return azureAdResponse.getAccess_token();
            } catch (final IOException e) {
                log.error(e.getMessage());
                throw new IllegalStateException("Klarte ikke deserialisere respons fra AzureAd", e);
            }
        } catch (final Exception e){
            log.error(e.getMessage());
            throw new IllegalArgumentException(e);
        }
    }
}
