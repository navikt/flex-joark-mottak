package no.nav.helse.flex.operations.eventenricher.saf;

import com.google.gson.Gson;
import io.vavr.CheckedFunction1;
import no.nav.helse.flex.infrastructure.MDCConstants;
import no.nav.helse.flex.infrastructure.resilience.Resilience;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.security.AzureAdClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

public class SafClient {
    private static final Logger log = LoggerFactory.getLogger(SafClient.class);
    private final static String SAF_JOURNALPOST_QUERY = "{journalpost(journalpostId: \"%s\") {" +
            " tittel" +
            " journalforendeEnhet" +
            " journalpostId" +
            " avsenderMottaker {" +
                " id" +
                " type" +
            " }" +
            " journalstatus" +
            " tema" +
            " behandlingstema" +
            " bruker {" +
            "   id" +
            "   type" +
            " }" +
            " relevanteDatoer {" +
            "   dato" +
            "   datotype" +
            " }" +
            " dokumenter {" +
            "   brevkode" +
            "   tittel" +
            "   dokumentInfoId" +
            " }" +
            "}}";
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";
    private static final String XSRF_TOKEN = "XSRF-TOKEN";
    private static final String QUERY_TAG = "query";
    private static final String SERVICENAME_SAF = "Saf";
    private static final int STATUS_OK = 200;
    private static final int NOT_AVAILABLE = 404;
    private final String safUrl;
    private final AzureAdClient azureAdClient;
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;

    public SafClient() {
        safUrl = Environment.getSafUrl();
        final CheckedFunction1<HttpRequest, HttpResponse<String>> safClientFunction = this::excecute;
        this.resilience = new Resilience<>(safClientFunction);
        this.azureAdClient = new AzureAdClient(Environment.getSafClientId());
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception{
        final HttpResponse<String> response= client.send(req, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    public Journalpost retriveJournalpost(final String journalpostId) throws ExternalServiceException, TemporarilyUnavailableException {
        final String correlationId = MDC.get(MDCConstants.CORRELATION_ID);
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(safUrl))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .header(X_XSRF_TOKEN, correlationId)
                .header("cookie", XSRF_TOKEN + "=" + correlationId)
                .POST(BodyPublishers.ofString(journalpostBody(journalpostId)))
                .build();
        final HttpResponse<String> response = resilience.execute(request);
        if(checkResponse(response, journalpostId)){
            return gson.fromJson(response.body(), SafResponse.class).getData().getJournalpost();
        } else {
            final SafErrorMessage safErrorMessage = gson.fromJson(response.body(), SafErrorMessage.class);
            log.error("Ved behandling av Journalpost {}: Feil ({})  {}; error: {}, message: {}", journalpostId, safErrorMessage.getStatus(), SERVICENAME_SAF, safErrorMessage.getError(), safErrorMessage.getMessage());
            throw new ExternalServiceException(SERVICENAME_SAF, safErrorMessage.getMessage(), safErrorMessage.getStatus());
        }
    }

    private String journalpostBody(final String journalpostId) {
        final String query = String.format(SAF_JOURNALPOST_QUERY, journalpostId);
        final JSONObject body = new JSONObject();
        body.put(QUERY_TAG, query);
        return body.toString();
    }

    private boolean checkResponse(final HttpResponse<String> response, final String journalpostId) throws TemporarilyUnavailableException {
        if (response.statusCode() == STATUS_OK) {
            return true;
        } else {
            if (response.statusCode() == NOT_AVAILABLE) {
                log.info("journalposten {} : JournalpostApi returnerte 404, tjeneste ikke tigjengelig.", journalpostId);
                throw new TemporarilyUnavailableException();
            }
            return false;
        }
    }
}
