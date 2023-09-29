package mock

import no.nav.helse.flex.KafkaEvent
import no.nav.helse.flex.journalpost.Journalpost
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
        behandlingstype = null,
    )
    val kafkaEvent = journalpost.tilKafkaEvent()
}

// Dette er de fleste dokumentene vi mottar og skal gjøre noe med
object PapirSoknadPerson {
    const val journalpostId = "22222222222"
    const val fnr = "22222222222"
    val journalpost = Journalpost(
        journalpostId = journalpostId,
        tittel = "TODO",
        journalstatus = "MOTTATT",
        bruker = Journalpost.Bruker(fnr, "PERSONBRUKER"),
        dokumenter = listOf(
            Journalpost.Dokument("NAV 08-07.04 D", "TODO", "123")
        ),
        tema = "SYK",
        journalforendeEnhet = "9999",
        relevanteDatoer = null,
        sak = null,
        avsenderMottaker = null,
        behandlingstema = null,
        behandlingstype = null,
    )
    val kafkaEvent = journalpost.tilKafkaEvent()
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
        behandlingstype = null,
    )
    val kafkaEvent = journalpost.tilKafkaEvent()
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
        behandlingstype = null,
    )
    val kafkaEvent = journalpost.tilKafkaEvent()
}

// Hentet Journalpost[id=620051103,tema=SYK,skjema=4936,tittel=Inntektsmelding,journalforendeEnhet=9999,journalstatus=JOURNALFOERT,behandlingstema=<null>] fra Saf

private fun Journalpost.tilKafkaEvent() = KafkaEvent(
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
