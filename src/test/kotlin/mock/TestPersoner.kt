package mock

import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.journalpost.Journalpost
import util.fromKClassToGenericRecord
import java.util.*

// Digital som har blitt arkivert av sykepengesoknad-arkivering-oppgave.
object DigitalSoknadPerson {
    const val JOURNALPOST_ID = "11111111111"
    const val FNR = "11111111111"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Søknad om sykepenger 01.01.2023 - 25.01.2023",
            journalstatus = "JOURNALFOERT",
            bruker = Journalpost.Bruker(FNR, "FNR"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 08-07.04 D", "Søknad om sykepenger 01.01.2023 - 25.01.2023", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = "9999",
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Papirsykepengesøkadn som dokument vi skal gjøre noe med.
object PapirSoknadPerson {
    const val JOURNALPOST_ID = "22222222222"
    const val FNR = "22222222222"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Sykmelding del D",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 08-07.04 D", "Sykmelding del D", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = "9999",
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Inntektsopplysninger for selvstendig næringsdrivende som skal ha JFR-oppgave.
object InntektsopplysningerPerson {
    const val JOURNALPOST_ID = "33333333333"
    const val FNR = "33333333333"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument(
                        "NAV 08-35.01",
                        "Inntektsopplysninger for selvstendig næringsdrivende og/eller frilansere som skal ha sykepenger",
                        "123",
                    ),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Klage som skal ha JFR-oppgave.
object KlagePerson {
    const val JOURNALPOST_ID = "928374927834"
    const val FNR = "33333333333"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Klage",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 90-00.08 K", "Klage", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Egenerklæring for utenlandske sykemeldinger som skal ha JFR-oppgave.
object UtenlandskPerson {
    const val JOURNALPOST_ID = "90384593875"
    const val FNR = "33333333333"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Egenerklæring for utenlandske sykemeldinger",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 08-09.06", "Egenerklæring for utenlandske sykemeldinger", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Journalpost som ikke har en brevkode skal ha JFR-oppgave.
object BrevløsPerson {
    const val JOURNALPOST_ID = "029834982"
    const val FNR = "33333333333"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Innsendingen gjelder: Sykepenger - Svar på henvendelse,journalforendeEnhet",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("", "Innsendingen gjelder: Sykepenger - Svar på henvendelse,journalforendeEnhet", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Papirsøknad hvor det er oppgitt org.nr i stedet for personnummer skal ha JFR-oppgave.
object PapirSoknadMedOrgNrPerson {
    const val JOURNALPOST_ID = "000000001"
    const val ORGNR = "000999000"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Sykmelding del D",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(ORGNR, "ORGNR"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 08-07.04 D", "Sykmelding del D", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = "9999",
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Journalpost hvor vi ikke finner behandlings-tema og -type skal ha JFR oppgave.
object UkjentBrevkodePerson {
    const val JOURNALPOST_ID = "984561332"
    const val FNR = "33333333333"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Annen post Utland",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 00-03.00 U", "Annen post Utland", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Journalpost som ikke er knyttet til en person.
object JournalpostUtenPerson {
    const val JOURNALPOST_ID = "787359875"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "NAV 08-09.06 Egenerklæring for utenlandske sykemeldinger",
            journalstatus = "MOTTATT",
            bruker = null,
            dokumenter =
                listOf(
                    Journalpost.Dokument("NAV 08-09.06", "NAV 08-09.06 Egenerklæring for utenlandske sykemeldinger", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

// Inntektsmelding vi ikke skal gjøre noe med.
object InntektsmeldingPerson {
    const val JOURNALPOST_ID = "44444444444"
    const val FNR = "44444444444"

    val journalpost =
        Journalpost(
            journalpostId = JOURNALPOST_ID,
            tittel = "Inntektsmelding",
            journalstatus = "MOTTATT",
            bruker = Journalpost.Bruker(FNR, "PERSONBRUKER"),
            dokumenter =
                listOf(
                    Journalpost.Dokument("4936", "Inntektsmelding", "123"),
                ),
            tema = "SYK",
            journalforendeEnhet = null,
            relevanteDatoer = null,
            sak = null,
            avsenderMottaker = null,
            behandlingstema = null,
            behandlingstype = null,
        )
    val kafkaEvent = journalpost.tilAvroKafkaEvent()
}

private fun Journalpost.tilAvroKafkaEvent() =
    fromKClassToGenericRecord(
        KafkaEvent(
            hendelsesId = UUID.randomUUID().toString(),
            hendelsesType =
                when (journalstatus) {
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
            behandlingstema = behandlingstema ?: "",
        ),
    )
