package no.nav.helse.flex.operations.generell.oppgave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Oppgave {

    private String id;
    private int versjon;
    private String beskrivelse;
    private String saksreferanse;
    private String status;

    public Oppgave() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

    public void setBeskrivelse(String beskrivelse){
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setSaksreferanse(String saksreferanse) {
        this.saksreferanse = saksreferanse;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
