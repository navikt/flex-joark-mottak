package no.nav.jfr.generell.infrastructure.exceptions;

public class ExternalServiceException extends Exception {
    private final String tjeneste;
    private final String feilmelding;
    private final String feilkode;

    public ExternalServiceException(final String tjeneste, final String feilmelding, final int feilkode) {
        super(feilmelding);
        this.tjeneste = tjeneste;
        this.feilmelding = feilmelding;
        this.feilkode = Integer.toString(feilkode);
    }

    public String getTjeneste() {
        return tjeneste;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public String getFeilkode() {
        return feilkode;
    }

    public String getFeilmeldingTilLogg() {
        return "Fikk feilkode (" + feilkode + ") under kall mot " + tjeneste + " med feilmelding:\n" + feilmelding;
    }
}
