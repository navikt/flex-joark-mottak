package no.nav.jfr.generell.operations.eventenricher.pdl;


import java.io.Serializable;

public class PdlResponse implements Serializable {

    private Data data;

    public PdlResponse() {}

    public Data getData() { return data; }

    public void setData(final Data data) {
        this.data = data;
    }

    HentIdenter getIdenter(){
        return data.getIdenter();
    }

    public String getGT(){
        return data.hentGeografiskTilknytning.getGT();
    }

    private class Data implements Serializable {
        private HentIdenter hentIdenter;
        private HentGeografiskTilknytning hentGeografiskTilknytning;

        public Data() {}

        public HentIdenter getIdenter() {
            return hentIdenter;
        }

        public void setIdenter(final HentIdenter identer) {
            this.hentIdenter = identer;
        }

        public HentGeografiskTilknytning getGeografiskTilknytning() {
            return hentGeografiskTilknytning;
        }
    }
}
