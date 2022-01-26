package no.nav.helse.flex.operations.generell.felleskodeverk;

public class Beskrivelse {
    private String term;
    private String tekst;

    public Beskrivelse() {}

    public Beskrivelse(final String term, final String tekst) {
        this.term = term;
        this.tekst = tekst;
    }

    public void setTerm(final String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

    public void setTekst(final String tekst) {
        this.tekst = tekst;
    }

    public String getTekst() {
        return tekst;
    }

    public TemaSkjemaData init() {
        final String[] tittelBrevkodeBehandlingstemaBehandlingstype = term.split(";", -1);
        return new TemaSkjemaData(
            tittelBrevkodeBehandlingstemaBehandlingstype[0],
            tittelBrevkodeBehandlingstemaBehandlingstype[1],
            tittelBrevkodeBehandlingstemaBehandlingstype[2],
            tittelBrevkodeBehandlingstemaBehandlingstype[3]
        );
    }
}
