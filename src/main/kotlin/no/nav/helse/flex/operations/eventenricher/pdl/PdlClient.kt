package no.nav.helse.flex.operations.eventenricher.pdl;

import com.google.gson.Gson;
import io.vavr.CheckedFunction1;
import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.MDCConstants;
import no.nav.helse.flex.infrastructure.exceptions.ExternalServiceException;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.resilience.Resilience;
import no.nav.helse.flex.infrastructure.security.AzureAdClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class PdlClient {
    private static final Logger log = LoggerFactory.getLogger(PdlClient.class);
    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String TEMA_HEADER = "tema";
    private static final String TJENESTE_PDL = "PDL";
    private static final int STATUS_OK = 200;
    private final String persondataUrl;
    private final Gson gson;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;
    private final AzureAdClient azureAdClient;


    private final String query = "{\"query\":\"query($ident: ID!, $grupper: [IdentGruppe!], $historikk:Boolean = false){ hentIdenter(ident: $ident, grupper:$grupper, historikk:$historikk){ identer{ident, historisk,gruppe}}}\"," +
            "\"variables\": {\"ident\":\"%s\",\"historikk\": false}}";

    public PdlClient() {
        persondataUrl = Environment.getPersondataUrl();
        final CheckedFunction1<HttpRequest, HttpResponse<String>> pdlClientFunction = this::excecute;
        this.resilience = new Resilience<>(pdlClientFunction);
        azureAdClient = new AzureAdClient(Environment.getPdlClientid());
        gson = new Gson();
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception{
        final HttpResponse<String> response= client.send(req, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    public List<Ident> retrieveIdenterFromPDL(String fnr, String tema, String journalpostId) throws Exception {
        String correlationId = MDC.get(MDCConstants.CORRELATION_ID);
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(persondataUrl))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .header(AUTHORIZATION_HEADER, azureAdClient.getToken())
                .header(CORRELATION_HEADER, correlationId)
                .header(TEMA_HEADER, tema)
                .POST(HttpRequest.BodyPublishers.ofString(persondataBody(query, fnr)))
                .build();
        final HttpResponse<String> response = resilience.execute(request);

        if (response.statusCode() == STATUS_OK) {
            try {
                final HentIdenter identer = gson.fromJson(response.body(), PdlResponse.class).getIdenter();
                return identer.getIdenter();
            } catch (final NullPointerException e) {
                log.error("Klarer ikke hente ut bruker i responsen fra PDL på journalpost {} - {}",journalpostId, e.getMessage());
                throw new Exception("Klarer ikke hente ut bruker i responsen fra PDL på journalpost",e);
            }
        }else if(response.statusCode() == 404 ||response.statusCode() == 503){
            throw new TemporarilyUnavailableException();
        }else {
            final String errorMessage = gson.fromJson(response.body(), PdlErrorResponse.class).getErrors().get(0).getMessage();
            log.error("Feil ved kall mot PDL på journalpost {}. Status: {} og feilmelding {}",journalpostId, response.statusCode(), errorMessage);
            throw new ExternalServiceException(TJENESTE_PDL, errorMessage, response.statusCode());
        }
    }

    private String persondataBody(final String query, final String fnr) {
        return String.format(query, fnr);
    }
}

