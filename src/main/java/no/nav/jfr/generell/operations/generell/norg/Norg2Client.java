package no.nav.jfr.generell.operations.generell.norg;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vavr.CheckedFunction1;
import no.nav.jfr.generell.Environment;
import no.nav.jfr.generell.infrastructure.exceptions.TemporarilyUnavailableException;
import no.nav.jfr.generell.infrastructure.resilience.Resilience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ServiceUnavailableException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;


public class Norg2Client {
    private final Logger log = LoggerFactory.getLogger((this.getClass()));
    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_AVAILABLE = 404;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final HttpClient client = HttpClient.newHttpClient();
    private final Resilience<HttpRequest, HttpResponse<String>> resilience;
    private final Gson gson;
    private final String norgUrl;

    public Norg2Client() {
        final CheckedFunction1<HttpRequest, HttpResponse<String>> norgClientFunction = this::excecute;
        this.resilience = new Resilience<>(norgClientFunction);
        this.norgUrl = Environment.getNorg2Url();
        gson = new Gson();
    }

    public List<String> fetchAktiveNavEnheter() throws ServiceUnavailableException, TemporarilyUnavailableException {
        Type navEnhetListType = new TypeToken<ArrayList<NAVEnhet>>(){}.getType();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(norgUrl + "enhet?enhetStatusListe=AKTIV"))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .GET()
                .build();
        HttpResponse<String> response = resilience.execute(request);
        if (response.statusCode() == STATUS_OK) {
            try {
                return mapNavEnhetListToEnhetNrList(gson.fromJson(response.body(), navEnhetListType));
            } catch (Exception e) {
                log.warn("Klarte ikke mappe response fra Norg2 til liste over aktive enheter: {}", e.getMessage());
                throw new TemporarilyUnavailableException();
            }
        }else{
            try{
                NorgErrorResponse errorResponse = gson.fromJson(response.body(), NorgErrorResponse.class);
                String errorMessage = errorResponse.getErrors().get(0).getMessage();
                log.error("Klarte ikke hente liste over aktive enheter fra NORG2 pga: {}", errorMessage);
            }catch (Exception e){
                log.error("Klarte ikke hente aktive enheter fra NORG2 OG klarte ikke mappe feilkoden");
            }
            throw new ServiceUnavailableException("");
        }
    }

    private List<String> mapNavEnhetListToEnhetNrList(List<NAVEnhet> navEnhetList) {
        List<String> navEnhetNrList = new ArrayList<>();
        for (NAVEnhet navEnhet : navEnhetList) {
            navEnhetNrList.add(navEnhet.getEnhetNr());
        }
        return navEnhetNrList;
    }

    public String fetchArbeidsfordelingEnhet(ArbeidsfordelingRequest arbeidsfordelingRequest, String jounalpostId) throws TemporarilyUnavailableException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(norgUrl + "arbeidsfordeling/enheter/bestmatch"))
                .header(CONTENT_TYPE_HEADER, "application/json")
                .headers("Nav-Consumer-Id", "Jfr-Infotrygd")
                .POST(HttpRequest.BodyPublishers.ofString(arbeidsfordelingRequest.toString()))
                .build();
        final HttpResponse<String> response = resilience.execute(request);
        if (response.statusCode() == STATUS_OK) {
            try {
                Type listType = new TypeToken<ArrayList<NAVEnhet>>(){}.getType();
                List<NAVEnhet> navEnhet = gson.fromJson(response.body(), listType);
                return navEnhet.get(0).getEnhetNr();
            } catch (final NullPointerException e) {
                log.error("Feil ved mapping av svar fra NORG2-arbeidsfordeling til journalpost {}: {} respons: {}", jounalpostId, response.body(), e.getMessage());
                throw new TemporarilyUnavailableException();
            }
        }else{
            try {
                final String errorMessage = gson.fromJson(response.body(), NorgErrorResponse.class).getErrors().get(0).getMessage();
                log.error("Feil ved kall mot Norg2 til journalpost {}: {}", jounalpostId, errorMessage);
                throw new TemporarilyUnavailableException();
            }catch (Exception e){
                log.error("Feil ved kall mot NORG2 OG klarte ikke serialisere svaret for journalpost {}", jounalpostId, e);
                throw new TemporarilyUnavailableException();
            }
        }
    }

    private HttpResponse<String> excecute(final HttpRequest req) throws Exception {
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
