package no.nav.helse.flex.operations.eventenricher.pdl;

public class HentGeografiskTilknytning {
    private String gtType;
    private String gtKommune;
    private String gtBydel;
    private String gtLand;

    public String getGtType() {
        return gtType;
    }

    public void setGtType(String gtType) {
        this.gtType = gtType;
    }

    public String getGtKommune() {
        return gtKommune;
    }

    public void setGtKommune(String gtKommune) {
        this.gtKommune = gtKommune;
    }

    public String getGtBydel() {
        return gtBydel;
    }

    public void setGtBydel(String gtBydel) {
        this.gtBydel = gtBydel;
    }

    public String getGtLand() {
        return gtLand;
    }

    public void setGtLand(String gtLand) {
        this.gtLand = gtLand;
    }

    public String getGT(){
        if(gtType.equalsIgnoreCase("KOMMUNE")){
            return this.gtKommune;
        }else if(gtType.equalsIgnoreCase("BYDEL")){
            return this.gtBydel;
        }else if(gtType.equalsIgnoreCase("UTLAND")){
            return this.gtLand;
        }else if(gtType.equalsIgnoreCase("UDEFINERT")){
            return this.gtType;
        }
        return null;
    }
}
