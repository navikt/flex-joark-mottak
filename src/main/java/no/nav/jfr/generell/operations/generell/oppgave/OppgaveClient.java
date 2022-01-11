package no.nav.jfr.generell.operations.generell.oppgave;

import com.google.gson.Gson;
import io.vavr.CheckedFunction1;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.exceptions.ExternalServiceException;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.jfr.generell.infrastructure.resilience.Resilience;
import no.nav.jfr.generell.infrastructure.security.AzureAdClient;
//import no.nav.jfr.generell.operations.generell.infotrygd.InfotrygdFacadeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static no.nav.jfr.generell.infrastructure.MDCConstants.CORRELATION_ID;

public class OppgaveClient {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final Logger log = LoggerFactory.getLogger(OppgaveClient.class);
    private static final String SERVICENAME_OPPGAVE = "Oppgave";
    private final Gson gson = new Gson();
    private final String oppgaveUrl;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;
    private final AzureAdClient azureAdClient;

    public OppgaveClient() {
        this.oppgaveUrl = Environment.getOppgaveUrl();
        final CheckedFunction1<HttpRequest, HttpResponse<String>> clientFunction = this::excecute;
        this.resilience = new Resilience<>(clientFunction);
        this.azureAdClient = new AzureAdClient(Environment.getOppgaveClientId());
    }

    public Oppgave createOppgave(final CreateOppgaveData requestData) throws ExternalServiceException, TemporarilyUnavailableException {
        final String correlationId = MDC.get(CORRELATION_ID);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(oppgaveUrl + "/api/v1/oppgaver"))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestData)))
                .build();
        final HttpResponse<String> response = resilience.execute(request);
        if (response.statusCode() == 201) {
            Oppgave oppgave = gson.fromJson(response.body(), Oppgave.class);
            return oppgave;
        } else if(response.statusCode() == 404){
            log.error("Klarte ikke opprette oppgave på journalpost {}, statuskode: {}", requestData.getJournalpostId(), response.statusCode());
            throw new TemporarilyUnavailableException();
        }else if( response.statusCode() >= 500 ) {
            final String errorText = hentFeilmelding(response);
            log.error("Klarte ikke opprette oppgave på journalpost {}, statuskode: {}. {}", requestData.getJournalpostId(), response.statusCode(), errorText);
            throw new TemporarilyUnavailableException();
        } else {
            final String errorText = hentFeilmelding(response);
            log.error("Klarte ikke opprette oppgave på journalpost {}, statuskode: {}. {}", requestData.getJournalpostId(), response.statusCode(), errorText);
            throw new ExternalServiceException(SERVICENAME_OPPGAVE, errorText, response.statusCode());
        }
    }

    public Oppgave updateOppgave(EnrichedKafkaEvent enrichedKafkaEvent) throws ExternalServiceException, TemporarilyUnavailableException {
        final String correlationId = MDC.get(CORRELATION_ID);
        Oppgave oppgave = enrichedKafkaEvent.getOppgave();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(oppgaveUrl + "/api/v1/oppgaver/"+oppgave.getId()))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(gson.toJson(oppgave)))
                .build();
        final HttpResponse<String> response = resilience.execute(request);

        if (response.statusCode() == 200) {
            Oppgave updatedOppgave = gson.fromJson(response.body(), Oppgave.class);
            return updatedOppgave;
        } else if (response.statusCode() == 404) {
            log.error("Klarte ikke oppdatere oppgave {} på journalpost {}, statuskode: {}", oppgave.getId(), enrichedKafkaEvent.getJournalpostId(), response.statusCode());
            throw new TemporarilyUnavailableException();
        } else if( response.statusCode() >= 500 ) {
            log.error("5XX fra oppgave {} {} {}", response, response.body(), response.body().toString());
            final String errorText = hentFeilmelding(response);
            log.error("Klarte ikke oppdatere oppgave {} på journalpost {}, statuskode: {}. {}", oppgave.getId(), enrichedKafkaEvent.getJournalpostId(), response.statusCode(), errorText);
            throw new TemporarilyUnavailableException();
        } else{
            log.error("Annen feil fra oppgave {} {} {}", response, response.body(), response.body().toString());
            final String errorText = hentFeilmelding(response);
            log.error("Klarte ikke oppdatere oppgave {} på journalpost {}, statuskode: {}. {}", oppgave.getId(), enrichedKafkaEvent.getJournalpostId(), response.statusCode(), errorText);
            throw new ExternalServiceException(SERVICENAME_OPPGAVE, "Feil under oppdatering av oppgave", response.statusCode());
        }
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception {
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private String hentFeilmelding(final HttpResponse<String> response) {
        final OppgaveErrorResponse oppgaveErrorResponse = gson.fromJson(response.body(), OppgaveErrorResponse.class);
        return oppgaveErrorResponse.getFeilmelding();
    }

}
