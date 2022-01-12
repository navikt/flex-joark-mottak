package no.nav.helse.flex.operations.generell.oppgave;

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
        LocalTime cutoffTime = LocalTime.of(12,0, 0);
        LocalTime now = LocalTime.now();
        Date oppgaveFrist = new Date();
        if(now.isAfter(cutoffTime)){
            oppgaveFrist = NorwegianDateUtil.addWorkingDaysToDate(oppgaveFrist, frist + 1);
        }else{
            oppgaveFrist = NorwegianDateUtil.addWorkingDaysToDate(oppgaveFrist, frist);
        }
        return oppgaveFrist.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }
}


