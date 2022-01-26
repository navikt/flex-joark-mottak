package no.nav.helse.flex.operations.eventenricher.pdl;


import java.util.List;

public class HentIdenter {
    private List<Ident> identer;

    public HentIdenter(final List<Ident> identer) {
        this.identer = identer;
    }

    public List<Ident> getIdenter() {
        return identer;
    }

    public void setIdenter(final List<Ident> identer) {
        this.identer = identer;
    }
}
