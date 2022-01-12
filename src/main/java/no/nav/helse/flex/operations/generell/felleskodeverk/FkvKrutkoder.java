package no.nav.helse.flex.operations.generell.felleskodeverk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FkvKrutkoder {
    private static final Logger log = LoggerFactory.getLogger(FkvKrutkoder.class);
    private static final String SKJEMA_MANGLER = "";
    private Map<String, List<Betydning>> betydninger;
    private final Map<String, TemaSkjemaData> temaSkjemaDataMap;

    public FkvKrutkoder() {
        temaSkjemaDataMap = new HashMap<>();
    }

    public FkvKrutkoder(final Map<String, List<Betydning>> betydninger) {
        this.betydninger = betydninger;
        temaSkjemaDataMap = new HashMap<>();
        init();
    }

    public void init() {
        final Set<String> betydningNokkler = betydninger.keySet();
        for (final String key : betydningNokkler) {
            final List<Betydning> betydningListe = betydninger.get(key);
            try {
                final Betydning betydning = betydningListe.get(0);
                temaSkjemaDataMap.put(key, betydning.init());
            } catch (final IndexOutOfBoundsException ie) {
                log.error("Feil ved dekoding av felles kodeverk, feil format i fkv - betydning ({}) - liste({})", key, betydningListe);
            }
        }
    }

    private TemaSkjemaData getTemaSkjema(final String temaSkjema) throws IllegalArgumentException {
        if (temaSkjemaDataMap.containsKey(temaSkjema.trim())) {
            return temaSkjemaDataMap.get(temaSkjema.trim());
        }
        return Optional.ofNullable(temaSkjemaDataMap.get(temaSkjema.trim())).orElseThrow(
                () -> new IllegalArgumentException("Skjema/Tema " + temaSkjema + " finnes ikke i kodeverket"));
    }

    private String lagTemaSkjemaNokkel(final String tema, final String skjema) {
        return skjema.trim() + ":" + tema.trim();
    }

    public String getBehandlingstype(final String tema, final String skjema) {
        if (tema == null || tema.isBlank() || skjema == null || skjema.isBlank()) {
            return SKJEMA_MANGLER;
        }
        return getTemaSkjema(lagTemaSkjemaNokkel(tema, skjema)).getBehandlingstype();
    }

    public String getBehandlingstema(final String tema, final String skjema) {
        if (tema == null || tema.isBlank() || skjema == null || skjema.isBlank()) {
            return SKJEMA_MANGLER;
        }
        return getTemaSkjema(lagTemaSkjemaNokkel(tema, skjema)).getBehandlingstema();
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder("TemaSkjemaRuting Felles kodeverk:\n");
        for (final String key : temaSkjemaDataMap.keySet()) {
            toString.append(key).append(",\n");
        }
        return toString.toString();
    }
}
