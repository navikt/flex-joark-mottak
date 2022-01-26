package no.nav.helse.flex.operations.eventenricher.journalpost;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Dokument {
    private String brevkode;
    private String tittel;
    private String dokumentInfoId;

    public Dokument(String brevkode, String tittel, String dokumentInfoId) {
        this.brevkode = brevkode;
        this.tittel = tittel;
        this.dokumentInfoId = dokumentInfoId;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getTittel() {
        return tittel;
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }
}
