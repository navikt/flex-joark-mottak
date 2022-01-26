package no.nav.helse.flex.operations.eventenricher.journalpost;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Journalpost {
    private static final String JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT = "M";
    private static final String JOURNALSTATUS_MOTTATT = "MOTTATT";
    private Bruker bruker;
    private String tittel;
    private String journalpostId;
    private String journalstatus;
    private List<Dokument> dokumenter;
    private String journalforendeEnhet;
    private List<RelevanteDatoer> relevanteDatoer;
    private Sak sak;
    private String tema;
    private AvsenderMottaker avsenderMottaker;
    private String behandlingstema;
    private String behandlingstype;

    public Journalpost() {
    }

    public String getBrevkode(){
        return dokumenter.size() > 0 ? dokumenter.get(0).getBrevkode(): "";
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public void setTittel(final String tittel) {
        this.tittel = tittel;
    }

    public void setJournalforendeEnhet(final String journalforendeEnhet) {
        this.journalforendeEnhet = journalforendeEnhet;
    }

    public void setBruker(final Bruker bruker) {
        this.bruker = bruker;
    }

    public void setDokumenter(final List<Dokument> dokumentliste) {
        this.dokumenter = dokumentliste;
    }

    public void setJournalstatus(final String journalstatus){
        this.journalstatus = journalstatus;
    }

    public String getJournalforendeEnhet() {
        return journalforendeEnhet;
    }

    public String getJournalstatus() {
        return journalstatus;
    }

    public List<Dokument> getDokumenter() {
        return dokumenter;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public boolean invalidJournalpostStatus(){
        return !(JOURNALSTATUS_MOTTATT.equals(journalstatus) || JOURNALSTATUS_MIDLERTIDIG_JOURNALFOERT.equals(journalstatus));
    }

    public void updateWithGenerellSak(){
        this.sak = new Sak();
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        return builder.append("{")
                .append("\"tittel\":\"").append(tittel).append("\",")
                .append("\"tema\":\"").append(tema).append("\",")
                .append(avsenderMottaker.toJson()).append(",")
                .append(sak.toJson()).append(",")
                .append(bruker.toJson())
                .append("}")
                .toString();
    }

    public String toString(){
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("id", journalpostId)
                .append("tema", tema)
                .append("skjema", getBrevkode())
                .append("tittel", tittel)
                .append("journalforendeEnhet", journalforendeEnhet)
                .append("journalstatus", journalstatus)
                .append("behandlingstema", behandlingstema)
                .toString();
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(String behandlingsTema) {
        this.behandlingstema = behandlingsTema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(String behandlingsType) {
        this.behandlingstype = behandlingsType;
    }

    public void settAvsenderMottaker(String brukerId, String idType) {
        avsenderMottaker = new AvsenderMottaker(brukerId, idType);
    }

    public AvsenderMottaker getAvsenderMottaker() {
        return avsenderMottaker;
    }

    public void setRelevanteDatoer(List<RelevanteDatoer> relevanteDatoer) {
        this.relevanteDatoer = relevanteDatoer;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bruker {
        private static final String AKTOERID = "AKTOERID";
        private static final String PERSONBRUKER = "FNR";
        private static final String ORGANISASJON = "ORGNR";
        private String id;
        private String type;

        public Bruker(final String id, final String type) {
            this.id = id;
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public boolean isAktoerId() {
            return AKTOERID.equals(type);
        }

        public boolean isFNR(){
            return PERSONBRUKER.equals(type);
        }

        public boolean isORGNR(){
            return ORGANISASJON.equals(type);
        }

        String toJson() {
            return "\"bruker\": {" +
                    "\"id\":\"" + id + "\"," +
                    "\"idType\":\"" + type + "\"" +
                    "}";
        }
    }

    public static class RelevanteDatoer {
        private String dato;
        private String datotype;

        public RelevanteDatoer(String dato, String datotype) {
            this.dato = dato;
            this.datotype = datotype;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sak {
        private static final String SAKSTYPE_GENERELL = "GENERELL_SAK";

        private final String sakstype;

        public Sak() {
            this.sakstype = SAKSTYPE_GENERELL;
        }

        String toJson() {
            return "\"sak\": {" +
                    "\"sakstype\":\"" + sakstype + "\"" +
                    "}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AvsenderMottaker {

        private String id;
        private String type;


        AvsenderMottaker(final String id, final String idType) {
            this.id = id;
            this.type = idType;
        }

        String toJson() {
            return "\"avsenderMottaker\": {" +
                    "\"id\":\"" + id + "\"," +
                    "\"idType\":\"" + type + "\"" +
                    "}";
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}

