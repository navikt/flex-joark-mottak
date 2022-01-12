package no.nav.helse.flex.infrastructure.kafka;

import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.eventenricher.pdl.Ident;
import no.nav.helse.flex.operations.generell.oppgave.Oppgave;

import java.util.List;
import java.util.UUID;

public class EnrichedKafkaEvent {
    private final KafkaEvent kafkaEvent;
    private final String correlationId;
    private Journalpost journalpost = null;
    private Oppgave oppgave;
    private List<Ident> identer;
    private String saksId;
    private int numFailedAttempts;
    private boolean toManuell = false;
    private boolean toIgnore = false;
    private boolean toFordeling = false;

    public EnrichedKafkaEvent(final KafkaEvent kafkaEvent) {
        this.correlationId = UUID.randomUUID().toString();
        this.kafkaEvent = kafkaEvent;
    }

    public int getNumFailedAttempts() {
        return this.numFailedAttempts;
    }

    public void incNumFailedAttempts() {
        this.numFailedAttempts += 1;
    }

    public Journalpost getJournalpost(){
        return this.journalpost;
    }

    public void setJournalpost(Journalpost journalpost) {
        this.journalpost = journalpost;
    }

    public KafkaEvent getKafkaEvent(){
        return this.kafkaEvent;
    }

    public String getCorrelationId(){
        return this.correlationId;
    }

    public String getJournalpostId(){
        return kafkaEvent.getJournalpostId();
    }

    public String getSkjema(){
        if(this.journalpost != null){
            return this.journalpost.getBrevkode();
        }
        return null;
    }

    public String getTema(){
        if (journalpost == null) {
            return kafkaEvent.getTemaNytt();
        }
        return journalpost.getTema();
    }

    public String getFnr() {
        if (journalpost.getBruker().isFNR()) {
            return journalpost.getBruker().getId();
        } else {
            for (Ident ident : identer) {
                if (ident.isFNR())
                    return ident.getID();
            }
        }
        return null;
    }

    public String getAktoerId() {
        if (journalpost.getBruker().isAktoerId()) {
            return journalpost.getBruker().getId();
        } else {
            for (Ident ident : identer) {
                if (ident.isAktoerId())
                    return ident.getID();
            }
        }
        return null;
    }

    public boolean isPersonbruker(){
        if(journalpost == null || journalpost.getBruker() == null){
            return false;
        }
        if(journalpost.getBruker().isORGNR()){
            return false;
        }
        return true;
    }

    public boolean isToManuell() {
        return toManuell;
    }

    public void setToManuell(boolean toManuell) {
        this.toManuell = toManuell;
    }

    public EnrichedKafkaEvent withSetToManuell(boolean toManuell){
        this.toManuell = toManuell;
        return this;
    }

    public Oppgave getOppgave() {
        return oppgave;
    }

    public void setOppgave(Oppgave oppgave) {
        this.oppgave = oppgave;
    }

    public boolean hasOppgave() {
        return oppgave != null;
    }

    public String getOppgaveId() {
        return oppgave.getId();
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public void setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
    }

    public void setIdenter(List<Ident> identer) {
        this.identer = identer;
    }

    public void setToFordeling(boolean toFordeling) {
        this.toFordeling = toFordeling;
    }
}
