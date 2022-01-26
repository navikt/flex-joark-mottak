package no.nav.helse.flex.operations.generell.felleskodeverk;

import java.util.Map;

public class Betydning {
    String gyldigFra;
    String gyldigTil;
    private Map<String, Beskrivelse> beskrivelser;

    Betydning() {}

    public Betydning(final String gyldigFra, final String gyldigTil, final Map<String, Beskrivelse> beskrivelser) {
        this.gyldigFra = gyldigFra;
        this.gyldigTil = gyldigTil;
        this.beskrivelser = beskrivelser;
    }

    public void setGyldigFra(final String gyldigFra) {
        this.gyldigFra = gyldigFra;
    }

    public String getGyldigFra() {
        return gyldigFra;
    }

    public void setGyldigTil(final String gyldigTil) {
        this.gyldigTil = gyldigTil;
    }

    public String getGyldigTil() {
        return gyldigTil;
    }

    public void setBeskrivelser(Map<String, Beskrivelse> beskrivelser) {
        this.beskrivelser = beskrivelser;
    }

    public Map<String, Beskrivelse> getBeskrivelser() {
        return beskrivelser;
    }

    public String getTekst(final String spraak) {
        return beskrivelser.get(spraak).getTekst();
    }

    public TemaSkjemaData init() {
        return beskrivelser.get("nb").init();
    }
}

