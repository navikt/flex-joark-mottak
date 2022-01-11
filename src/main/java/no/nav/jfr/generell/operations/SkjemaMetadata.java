package no.nav.jfr.generell.operations;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import no.nav.jfr.generell.Environment;

import javax.validation.constraints.Null;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class SkjemaMetadata {
    private final HashMap<String, TemaKodeverk> temaMap;

    public SkjemaMetadata() {
        final Gson gson = new Gson();
        String jsonString = Environment.getSkjemaerJson();
        this.temaMap = gson.fromJson(jsonString, new TypeToken<HashMap<String, TemaKodeverk>>(){}.getType());
    }

    public boolean inTemaList(String tema){
        //TODO:Fjern når YRK tar over i produksjon
        if(!Environment.erProd() && tema.contains("YRK")) return false;
        return temaMap.containsKey(tema);
    }

    public boolean acceptedKanal(String tema, String kanal){
        List<String> ignoreKanal = temaMap.get(tema).getIgnoreKanal();
        ignoreKanal.add("EESSI");
        return !(ignoreKanal.contains(kanal));
    }

    public boolean inAutoList(String tema, String skjema){
        if(temaMap.containsKey(tema)){
            return temaMap.get(tema).hasSkjema(skjema);
        }
        return false;
    }

    public int getFrist(String tema, String skjema){
        TemaKodeverk temaKodeverk = temaMap.get(tema);
        return temaKodeverk.getFristFromSkjema(skjema);
    }

    public String getOppgavetype(String tema, String skjema){
        TemaKodeverk temaKodeverk = temaMap.get(tema);
        return temaKodeverk.getOppgavetypeFromSkjema(skjema);
    }

    public boolean isIgnoreskjema(String tema, String skjema){
        return temaMap.get(tema).isIgnoreSkjema(skjema);
    }

    private class TemaKodeverk {
        private HashMap<String, SkjemaKodeverk> skjemaer;
        private List<String> ignoreKanal;
        private List<String> ignoreSkjema;

        public boolean hasSkjema(final String skjema) {
            if(skjemaer == null || skjemaer.isEmpty()){
                return false;
            }
            return skjemaer.containsKey(Objects.requireNonNullElse(skjema, "null"));
        }

        public boolean isIgnoreSkjema(String skjema){
            if(ignoreSkjema == null || ignoreSkjema.isEmpty()){
                return false;
            }
            return ignoreSkjema.contains(skjema);
        }

        public List<String> getIgnoreKanal(){
            if(ignoreKanal != null){
                return ignoreKanal;
            }
            return new ArrayList<String>();
        }

        public int getFristFromSkjema(String skjema){
            return skjemaer.get(skjema).getFrist();
        }

        public String getOppgavetypeFromSkjema(String skjema){
            return skjemaer.get(skjema).getOppgavetype();
        }

        private class SkjemaKodeverk {
            private int frist;
            private String oppgavetype;

            public SkjemaKodeverk(int frist, String oppgavetype) {
                this.frist = frist;
                this.oppgavetype = oppgavetype;
            }

            public int getFrist() {
                return frist;
            }

            public String getOppgavetype() {
                return oppgavetype;
            }
        }
    }
}
