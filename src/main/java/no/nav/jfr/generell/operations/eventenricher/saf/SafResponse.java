package no.nav.jfr.generell.operations.eventenricher.saf;

import no.nav.jfr.generell.operations.eventenricher.journalpost.Journalpost;

import java.io.Serializable;

public class SafResponse implements Serializable {
    private Data data;

    public SafResponse() {}

    public Data getData() { return data; }

    public void setData(final Data data) {
        this.data = data;
    }

    public class Data implements Serializable {
        private Journalpost journalpost;

        public Data() {}

        public Journalpost getJournalpost() {
            return journalpost;
        }

        public void setJournalpost(final Journalpost journalpost) {
            this.journalpost = journalpost;
        }
    }
}
