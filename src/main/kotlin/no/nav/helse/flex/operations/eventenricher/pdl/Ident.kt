package no.nav.helse.flex.operations.eventenricher.pdl;

public class Ident {
    private final static String AKTOERID = "AKTORID";
    private final static String FNR = "FOLKEREGISTERIDENT";
    private String ident;
    private boolean historisk;
    private String gruppe;

    public Ident(String ident, boolean historisk, String gruppe){
        this.ident = ident;
        this.historisk = historisk;
        this.gruppe = gruppe;
    }

    public String getID() {
        return ident;
    }

    public boolean isAktoerId() {
        return AKTOERID.equals(gruppe);
    }

    public boolean isFNR() {
        return FNR.equals(gruppe);
    }
}
