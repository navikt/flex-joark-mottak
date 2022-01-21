package no.nav.helse.flex.operations;

import no.bekk.bekkopen.date.NorwegianDateUtil;
import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent;
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent;
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument;
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost;
import no.nav.helse.flex.operations.eventenricher.pdl.Ident;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TestUtils {

    public static Journalpost mockJournalpost(String journalpostId, String brevkode, String tema, String journalpostStatus){
        Journalpost.Bruker mockBruker;

        mockBruker = new Journalpost.Bruker("10108000398", "FNR");

        Journalpost mockJournalpost = new Journalpost();
        mockJournalpost.setTittel("Test Journalpost");
        mockJournalpost.setJournalpostId(journalpostId);
        mockJournalpost.setJournalforendeEnhet("1111");
        mockJournalpost.setDokumenter(Collections.singletonList(new Dokument(brevkode, "DokTittel", "123")));
        mockJournalpost.setRelevanteDatoer(Collections.singletonList(new Journalpost.RelevanteDatoer("2021-10-20T20:20:00", "DATO_REGISTRERT")));
        mockJournalpost.setBruker(mockBruker);
        mockJournalpost.setJournalstatus(journalpostStatus);
        mockJournalpost.setTema(tema);

        return mockJournalpost;
    }

    public static EnrichedKafkaEvent mockEnrichedKafkaevent(){
        KafkaEvent kafkaEvent = new KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO");
        EnrichedKafkaEvent enrichedKafkaEvent = new EnrichedKafkaEvent(kafkaEvent);
        Journalpost journalpost = mockJournalpost("123456789", "NAV 08-07.04 D", "SYK", "M");
        enrichedKafkaEvent.setJournalpost(journalpost);
        enrichedKafkaEvent.setIdenter(List.of(new Ident("1122334455", false, "AKTORID")));
        return enrichedKafkaEvent;
    }

    public static GenericRecord mockJournalpostEvent(String tema){
        String journalHendelseSchema = "{" +
                "  \"namespace\" : \"no.nav.joarkjournalfoeringhendelser\"," +
                "  \"type\" : \"record\"," +
                "  \"name\" : \"JournalfoeringHendelseRecord\"," +
                "  \"fields\" : [\n" +
                "    {\"name\": \"hendelsesId\", \"type\": \"string\"}," +
                "    {\"name\": \"versjon\", \"type\": \"int\"}," +
                "    {\"name\": \"hendelsesType\", \"type\": \"string\"}," +
                "    {\"name\": \"journalpostId\", \"type\": \"long\"}," +
                "    {\"name\": \"journalpostStatus\", \"type\": \"string\"}," +
                "    {\"name\": \"temaGammelt\", \"type\": \"string\"}," +
                "    {\"name\": \"temaNytt\", \"type\": \"string\"}," +
                "    {\"name\": \"mottaksKanal\", \"type\": \"string\"}," +
                "    {\"name\": \"kanalReferanseId\", \"type\": \"string\"}," +
                "    {\"name\": \"behandlingstema\", \"type\": \"string\", \"default\": \"\"}" +
                "  ]" +
                "}";

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(journalHendelseSchema);
        GenericRecord avroRecord = new GenericData.Record(schema);
        avroRecord.put("journalpostId", 123456789);
        avroRecord.put("journalpostStatus", "M");
        avroRecord.put("hendelsesId", "1234234345");
        avroRecord.put("hendelsesType", "JournalpostMottatt");
        avroRecord.put("versjon", 1);
        avroRecord.put("temaNytt", tema);
        avroRecord.put("temaGammelt", "");
        avroRecord.put("kanalReferanseId", "");
        avroRecord.put("mottaksKanal", "NAV_NO");
        avroRecord.put("behandlingstema", "");



        return avroRecord;
    }

    public static boolean InfotrygdClosed(){
        LocalTime time = LocalTime.now();
        Date date = new Date();
        boolean isWorkingDay = NorwegianDateUtil.isWorkingDay(date);
        boolean isWorktime =
                time.isAfter(LocalTime.parse("06:30:00")) &&
                        time.isBefore(LocalTime.parse("20:00:00"));
        return !(isWorkingDay && isWorktime);
    }
}
