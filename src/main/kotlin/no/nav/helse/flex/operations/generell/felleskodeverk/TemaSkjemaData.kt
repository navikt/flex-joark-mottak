package no.nav.helse.flex.operations.generell.felleskodeverk;

public class TemaSkjemaData {
    final String brevkode;
    final String tittel;
    final String behandlingstema;
    final String behandlingstype;

    TemaSkjemaData(final String tittel, final String brevkode,
                   final String behandlingstema, final String behandlingstype) {
        this.brevkode = brevkode;
        this.tittel = tittel;
        this.behandlingstema = behandlingstema;
        this.behandlingstype = behandlingstype;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getTittel() {
        return tittel;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public boolean erManglendeSkjema() {
        return "".equals(tittel);
    }
}
