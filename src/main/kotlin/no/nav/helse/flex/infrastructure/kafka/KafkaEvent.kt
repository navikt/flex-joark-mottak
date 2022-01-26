package no.nav.helse.flex.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaEvent {
    private String hendelsesId; //CorrolationId
    private int versjon;
    private String hendelsesType;
    private long journalpostId;
    private String journalpostStatus;
    private String temaGammelt;
    private String temaNytt;
    private String mottaksKanal;
    private String kanalReferanseId;
    private String behandlingstema;

    public KafkaEvent(String hendelsesId, String hendelsesType, long journalpostId, String temaNytt, String mottaksKanal) {
        this.hendelsesId = hendelsesId;
        this.hendelsesType = hendelsesType;
        this.journalpostId = journalpostId;
        this.temaNytt = temaNytt;
        this.mottaksKanal = mottaksKanal;
    }

    public String getJournalpostId() {
        return String.valueOf(journalpostId);
    }

    public String getJournalpostStatus() {
        return journalpostStatus;
    }

    public String getTemaNytt() {
        return temaNytt;
    }

    public String getMottaksKanal() {
        return mottaksKanal;
    }

}
