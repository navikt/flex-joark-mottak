package no.nav.jfr.generell.operations.generell.oppgave;

public class OppgaveErrorResponse {
    private String uuid;
    private String feilmelding;

    public OppgaveErrorResponse() {}


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }
}
