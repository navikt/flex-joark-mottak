package no.nav.jfr.generell.operations.generell.oppgave;

import no.bekk.bekkopen.date.NorwegianDateUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

public class CreateOppgaveData {
    private String aktoerId;
    private String journalpostId;
    private String tema;
    private String oppgavetype;
    private String behandlingstema;
    private String behandlingstype;
    private String prioritet;
    private String aktivDato;
    private String fristFerdigstillelse;
    private String tildeltEnhetsnr;

    public CreateOppgaveData(String aktoerId, String journalpostId, String tema, String behandlingstema, String behandlingstype, String oppgavetype, int frist) {
        this.tema = tema;
        this.aktoerId = aktoerId;
        this.journalpostId = journalpostId;
        this.behandlingstema = behandlingstema;
        this.behandlingstype = behandlingstype;
        this.aktivDato = LocalDate.now().toString();
        this.oppgavetype = oppgavetype;
        this.prioritet = "NORM";
        this.fristFerdigstillelse = nextValidFrist(frist);
    }

    public void setTildeltEnhetsnr(String tildeltEnhetsnr) {
        this.tildeltEnhetsnr = tildeltEnhetsnr;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    private String nextValidFrist(int frist){
        //ToDo: gir ikke dette mening? (selv om det ikke er eksakt slikg det er i dag)
        // Før kl. 12: setter vi frist til om x antall dager
        // Etter kl. 12: setter vi frist til om x dager + 1 arbeidsdag
        // På en måte det vi gjør med JFR oppgave hvor "x dager" er 0.
        LocalTime cutoffTime = LocalTime.of(12,0, 0);
        LocalTime now = LocalTime.now();
        Date oppgaveFrist = new Date();
        if(now.isAfter(cutoffTime)){
            oppgaveFrist = NorwegianDateUtil.addWorkingDaysToDate(oppgaveFrist, frist+1);
        }else{
            oppgaveFrist = NorwegianDateUtil.addWorkingDaysToDate(oppgaveFrist, frist);
        }
        return oppgaveFrist.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }
}


