package mock

import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.journalpost.Journalpost
import util.fromKClassToGenericRecord
import java.lang.Exception
import java.util.*

// Dette er vanlige/digitale søknader som er blitt arkivert av sykepengesoknad-arkivering-oppgave
object DigitalSoknadPerson {
    const val journalpostId = "11111111111"
    const val fnr = "11111111111"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Søknad om sykepenger 01.01.2023 - 25.01.2023",
        journalstatus = "JOURNALFOERT",
        bruker = Journalpost.Bruker(fnr, "FNR"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-07.04 D", "Søknad om sykepenger 01.01.2023 - 25.01.2023", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = "9999",
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Dette er de fleste dokumentene vi mottar og skal gjøre noe med
object PapirSoknadPerson {
    const val journalpostId = "22222222222"
    const val fnr = "22222222222"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Sykmelding del D",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-07.04 D", "Sykmelding del D", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = "9999",
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Dette er inntektsopplysninger for selvstendig næringsdrivende som skal ha JFR oppgave
object InntektsopplysningerPerson {
    const val journalpostId = "33333333333"
    const val fnr = "33333333333"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-35.01", "Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Dette er en klage som skal ha JFR oppgave
object KlagePerson {
    const val journalpostId = "928374927834"
    const val fnr = "33333333333"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Klage",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 90-00.08 K", "Klage", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Egenerklæring for utenlandske sykemeldinger som skal ha JFR oppgave
object UtlanskPerson {
    const val journalpostId = "90384593875"
    const val fnr = "33333333333"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Egenerklæring for utenlandske sykemeldinger",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-09.06", "Egenerklæring for utenlandske sykemeldinger", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Journalpost som ikke har en brevkode skal ha JFR oppgave
object BrevløsPerson {
    const val journalpostId = "029834982"
    const val fnr = "33333333333"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Innsendingen gjelder: Sykepenger - Svar på henvendelse,journalforendeEnhet",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("", "Innsendingen gjelder: Sykepenger - Svar på henvendelse,journalforendeEnhet", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Journalpost som ikke er knyttet til en person
object JournalpostUtenPerson {
    const val journalpostId = "787359875"
    const val fnr = "33333333333"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "NAV 08-09.06 Egenerklæring for utenlandske sykemeldinger",
        journalstatus = "MOTTATT",
        bruker = null,
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-09.06", "NAV 08-09.06 Egenerklæring for utenlandske sykemeldinger", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Dette er inntektsmelding som vi ikke skal gjøre noe med
object InntektsmeldingPerson {
    const val journalpostId = "44444444444"
    const val fnr = "44444444444"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "Inntektsmelding",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("4936", "Inntektsmelding", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = null,
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null
    )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

private fun Journalpost.tilAvroKafkaEvent() = fromKClassToGenericRecord(
    KafkaEvent(
        hendelsesId = UUID.randomUUID().toString(),
        hendelsesType = when (journalstatus) {
            "MOTTATT" -> "Mottatt"
            "JOURNALFOERT" -> "Mottatt"
            else -> throw Exception("Mangler journalpost mapping")
        },
        journalpostId = journalpostId,
        temaNytt = tema,
        temaGammelt = tema,
        mottaksKanal = "NAV_NO",
        journalpostStatus = journalstatus,
        versjon = 1,
        kanalReferanseId = UUID.randomUUID().toString(),
        behandlingstema = behandlingstema ?: ""
    )
)
