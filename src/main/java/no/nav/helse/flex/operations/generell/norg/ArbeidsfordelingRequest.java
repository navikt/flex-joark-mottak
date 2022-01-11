package no.nav.helse.flex.operations.generell.norg;

public class ArbeidsfordelingRequest {

    private String tema;
    private String geografiskTilknytning;
    private String behandlingstema;
    private String behandlingstype;
    private String oppgavetype;
    private String diskresjonskode;
    private Boolean egenAnsatt;

    public String getTema() {
        return tema;
    }

    public String getGeografiskTilknytning() {
        return geografiskTilknytning;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    public Boolean erEgenAnsatt() {
        return egenAnsatt;
    }

    public ArbeidsfordelingRequest withEgenAnsatt(boolean egenAnsatt) {
        this.egenAnsatt = egenAnsatt;
        return this;
    }

    public ArbeidsfordelingRequest withTema(String tema) {
        this.tema = tema;
        return this;
    }

    public ArbeidsfordelingRequest withGeografiskTilknytning(String geografiskTilknytning) {
        this.geografiskTilknytning = geografiskTilknytning;
        return this;
    }

    public ArbeidsfordelingRequest withBehandlingstema(String behandlingstema) {
        this.behandlingstema = behandlingstema;
        return this;
    }

    public ArbeidsfordelingRequest withBehandlingstype(String behandlingstype) {
        this.behandlingstype = behandlingstype;
        return this;
    }

    public ArbeidsfordelingRequest withOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
        return this;
    }

    public ArbeidsfordelingRequest withDiskresjonskodeFromAdresseskjerming(String adresseskjerming) {
        if("FORTROLIG".equalsIgnoreCase(adresseskjerming)){
            this.diskresjonskode = "SPFO";
        }else if("STRENGT_FORTROLIG".equalsIgnoreCase(adresseskjerming)){
            this.diskresjonskode = "SPSF";
        }else if("STRENGT_FORTROLIG_UTLAND".equalsIgnoreCase(adresseskjerming)){
            this.diskresjonskode = "SPSF";
        }else{
            this.diskresjonskode = null;
        }
        return this;
    }

    public ArbeidsfordelingRequest withDiskresjonskode(String diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{")
                .append("\"tema\":\"").append(tema).append("\",")
                .append("\"oppgavetype\":\"").append(oppgavetype).append("\",")
                .append("\"geografiskOmraade\":\"").append(geografiskTilknytning).append("\",");
        if(behandlingstema != null && !behandlingstema.equalsIgnoreCase("")){
            builder.append("\"behandlingstema\":\"").append(behandlingstema).append("\",");
        }
        if(behandlingstype != null && !behandlingstype.equalsIgnoreCase("")){
            builder.append("\"behandlingstype\":\"").append(behandlingstype).append("\",");
        }
        if(diskresjonskode != null){
            builder.append("\"diskresjonskode\":\"").append(diskresjonskode).append("\",");
        }
        builder.append("\"skjermet\":\"").append(egenAnsatt).append("\"").append("}");
        return builder.toString();
    }

}
