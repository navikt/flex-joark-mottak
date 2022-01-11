package no.nav.jfr.generell.operations.journalforing.dokarkiv;

import io.vavr.CheckedFunction1;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.exceptions.ExternalServiceException;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.resilience.Resilience;
import no.nav.jfr.generell.infrastructure.security.AzureAdClient;
import no.nav.jfr.generell.operations.eventenricher.journalpost.Journalpost;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static no.nav.jfr.generell.infrastructure.MDCConstants.CORRELATION_ID;

public class JournalpostAPIClient {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String APPLICATION = "journalpostApi";
    private static final Logger log = LoggerFactory.getLogger(JournalpostAPIClient.class);
    private final String journalpostApiUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;
    private final AzureAdClient azureAdClient;

    public JournalpostAPIClient(){
        this.journalpostApiUrl = Environment.getJournalpostApiUrl();
        final CheckedFunction1<HttpRequest, HttpResponse<String>> clientFunction = this::excecute;
        this.resilience = new Resilience<>(clientFunction);
        this.azureAdClient = new AzureAdClient(Environment.getDokarkivClientId());
    }


    public void finalizeJournalpost(final String journalpostId) throws ExternalServiceException, TemporarilyUnavailableException {
        final String correlationId = MDC.get(CORRELATION_ID);
        final JSONObject requestbody = new JSONObject().put("journalfoerendeEnhet", "9999");
        try{
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(journalpostApiUrl +"/"+ journalpostId + "/ferdigstill"))
                    .header(CONTENT_TYPE_HEADER, "application/json")
                    .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                    .header(CORRELATION_HEADER, correlationId)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(requestbody.toString()))
                    .build();
            final HttpResponse<String> response = resilience.execute(request);
            if(response.statusCode() == 200){
            }else if(response.statusCode() == 503 || response.statusCode() == 404){
                log.error("Feilkode mot Journalpost API {}", response.statusCode());
                throw new TemporarilyUnavailableException();
            } else {
                log.error("Feilkode fra JournalpostApi: {} - ferdigstill body - {}", response.statusCode(), response.body());
                throw new ExternalServiceException("JournalpostApi", "Kunne ikke ferdigstille Journalpsot", response.statusCode());
            }
        }catch (Exception e){
            log.error("Ukjent feil mot ferdigstill journalpost på journalpost {}", journalpostId, e);
            throw new TemporarilyUnavailableException();
        }
    }

    public void updateJournalpost(final Journalpost journalpost) throws ExternalServiceException, TemporarilyUnavailableException {
        final String correlationId = MDC.get(CORRELATION_ID);
        final String journalpostId = journalpost.getJournalpostId();

        String jsonBody = journalpost.toJson();
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(journalpostApiUrl + "/" + journalpostId))
                    .header(CONTENT_TYPE_HEADER, "application/json")
                    .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                    .header(CORRELATION_HEADER, correlationId)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            final HttpResponse<String> response = resilience.execute(request);
            if(response.statusCode() == 200){
            }else if(response.statusCode() == 503 || response.statusCode() == 404){
                throw new TemporarilyUnavailableException();
            } else {
                log.error("Feilkode fra JournalpostApi: {} {} - oppdater", response.statusCode(), response.body());
                throw new ExternalServiceException(APPLICATION, "Kunne ikke oppdatere Journalpsot", response.statusCode());
            }
        }catch (Exception e){
            log.error("Ukjent feil mot oppdater journalpost på journalpost {}", journalpost.getJournalpostId(), e);
            throw new TemporarilyUnavailableException();
        }
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception{
        final HttpResponse<String> response= client.send(req, HttpResponse.BodyHandlers.ofString());
        return response;
    }

}
