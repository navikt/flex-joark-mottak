package util

import no.nav.helse.flex.infrastructure.kafka.EnrichedKafkaEvent
import no.nav.helse.flex.infrastructure.kafka.KafkaEvent
import no.nav.helse.flex.operations.eventenricher.journalpost.Dokument
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost.Bruker
import no.nav.helse.flex.operations.eventenricher.journalpost.Journalpost.RelevanteDatoer
import no.nav.helse.flex.operations.eventenricher.pdl.Ident
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import java.util.*

object TestUtils {
    fun mockJournalpost(event: KafkaEvent) = mockJournalpost(
        journalpostId = event.getJournalpostId(),
        brevkode = "NAV 08-07.04 D",
        tema = event.temaNytt,
        journalpostStatus = event.journalpostStatus
    )

    fun mockJournalpost(
        journalpostId: String,
        brevkode: String?,
        tema: String,
        journalpostStatus: String
    ): Journalpost {
        val mockJournalpost = Journalpost()
        mockJournalpost.tittel = "Test Journalpost"
        mockJournalpost.journalpostId = journalpostId
        mockJournalpost.journalforendeEnhet = "1111"
        mockJournalpost.dokumenter = listOf(Dokument(brevkode, "DokTittel", "123"))
        mockJournalpost.relevanteDatoer = listOf(RelevanteDatoer("2021-10-20T20:20:00", "DATO_REGISTRERT"))
        mockJournalpost.bruker = Bruker("10108000398", "FNR")
        mockJournalpost.journalstatus = journalpostStatus
        mockJournalpost.tema = tema

        return mockJournalpost
    }

    fun mockEnrichedKafkaevent(): EnrichedKafkaEvent {
        val kafkaEvent = KafkaEvent(UUID.randomUUID().toString(), "Mottatt", 123456789, "SYK", "NAV_NO", "M")
        val enrichedKafkaEvent = EnrichedKafkaEvent(kafkaEvent)
        val journalpost = mockJournalpost("123456789", "NAV 08-07.04 D", "SYK", "M")
        enrichedKafkaEvent.journalpost = journalpost
        enrichedKafkaEvent.identer = listOf(Ident("AKTORID", "1122334455"))
        return enrichedKafkaEvent
    }

    fun mockJournalpostEvent(tema: String?): GenericRecord {
        val journalHendelseSchema =
            """{  
                "namespace" : "no.nav.joarkjournalfoeringhendelser",  "type" : "record",
                "name" : "JournalfoeringHendelseRecord",
                "fields" : [
                    {"name": "hendelsesId", "type": "string"},
                    {"name": "versjon", "type": "int"},
                    {"name": "hendelsesType", "type": "string"},
                    {"name": "journalpostId", "type": "long"},
                    {"name": "journalpostStatus", "type": "string"},
                    {"name": "temaGammelt", "type": "string"},
                    {"name": "temaNytt", "type": "string"},
                    {"name": "mottaksKanal", "type": "string"},
                    {"name": "kanalReferanseId", "type": "string"},
                    {"name": "behandlingstema", "type": "string", "default": ""}
                ]
            }"""
        val parser = Schema.Parser()
        val schema = parser.parse(journalHendelseSchema)
        val avroRecord: GenericRecord = GenericData.Record(schema)
        avroRecord.put("journalpostId", 573783667)
        avroRecord.put("journalpostStatus", "M")
        avroRecord.put("hendelsesId", "1234234345")
        avroRecord.put("hendelsesType", "JournalpostMottatt")
        avroRecord.put("versjon", 1)
        avroRecord.put("temaNytt", tema)
        avroRecord.put("temaGammelt", "")
        avroRecord.put("kanalReferanseId", "")
        avroRecord.put("mottaksKanal", "NAV_NO")
        avroRecord.put("behandlingstema", "")
        return avroRecord
    }
}
