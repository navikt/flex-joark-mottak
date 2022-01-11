package no.nav.helse.flex.operations.generell.felleskodeverk;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.vavr.CheckedFunction1;
import no.nav.helse.flex.Environment;
import no.nav.helse.flex.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.helse.flex.infrastructure.resilience.Resilience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ServiceUnavailableException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FkvClient {
    private final Logger log = LoggerFactory.getLogger(FkvClient.class);
    private static final String CORRELATION_HEADER = "Nav-Call-Id";
    private static final String NAV_CONSUMER_ID = "Nav-Consumer-Id";
    private final String fellesKodeverkUrl;
    private final Gson gson;
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;

    public FkvClient() {
        this.fellesKodeverkUrl = Environment.getFkvUrl();
        final CheckedFunction1<HttpRequest, HttpResponse<String>> pdlClientFunction = this::excecute;
        this.resilience = new Resilience<>(pdlClientFunction);
        this.gson = new Gson();
    }

    public FkvKrutkoder fetchKrutKoder() throws Exception {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fellesKodeverkUrl))
                    .header(CORRELATION_HEADER, "jfr-manuell-oppretter-oppstart")
                    .header(NAV_CONSUMER_ID, "srvJfr-manuell-opp")
                    .GET()
                    .build();
            final HttpResponse<String> response = resilience.execute(request);
            if(response.statusCode() == 200){
                return mapFKVStringToObject(response.body());
            }else {
                log.error("Klarte ikke hente Krutkoder fra Felles kodeverk");
                throw new TemporarilyUnavailableException();
            }
        } catch (Exception e) {
            log.error("Feil ved henting/parsing av KrutKoder: {}" + e.getMessage(), e);
            throw new ServiceUnavailableException();
        }
    }

    private FkvKrutkoder mapFKVStringToObject(String fellesKodeverkJson) throws ServiceUnavailableException {
        try {
            FkvKrutkoder fellesKodeverk = gson.fromJson(fellesKodeverkJson, FkvKrutkoder.class);
            fellesKodeverk.init();
            return fellesKodeverk;
        } catch (final JsonParseException e) {
            throw new ServiceUnavailableException("Feil under dekoding av melding fra felles kodeverk: " + fellesKodeverkJson);
        }
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception {
        final HttpResponse<String> response= client.send(req, HttpResponse.BodyHandlers.ofString());
        return response;
    }
}
