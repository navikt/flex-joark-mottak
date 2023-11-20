# flex-joark-mottak

Applikasjonen tar over for [jfr-generell](https://github.com/navikt/jfr-generell) på journalposter med tema SYK.

Lytter på journalpost dokumenter som legges på topicet `teamdokumenthandtering.aapen-dok-journalfoering`. 
Oppretter enten SOK oppgaver for sykepengesøknader sendt på papir eller JFR oppgaver for de fleste andre journalposter på tema SYK.

## Testing i dev
Her kan man opprette journalpost [flex-testdata-generator](https://flex-testdata-generator.intern.dev.nav.no/papir-dokument)
> Bruk denne testdataen:
>
> `Fødselsnummer: fnr i fra dolly` 
> 
> `Tema: SYK`
> 
> `Skjema: NAV 08-07.04D` = Ferdigstilt automatisk
>
> `Skjema: NAV 90-00.08 K` = Journaløringsoppgave

Ta med journalpost id og sjekk resultatet [her](https://gosys-q1.dev.intern.nav.no/gosys/dokument/sokjournalpost.jsf) og inne på J.post finner man tilhørende oppgave.
Logg inn med felles syfo Z992389 bruker, eller opprett egen i [IDA](https://confluence.adeo.no/display/ATOM/IDA).

## Felles kodeverk - krutkoder
Består av `<brevkode>:<tema>` = `<tittel>;<brevkode>;<behandlingstema>;<behandlingstype>;` og det er 49 forskjellige som gjelder for tema SYK

Listen finnes [her](https://kodeverk-web.dev.intern.nav.no/kodeverksoversikt/kodeverk/Krutkoder) hvis man logger inn med Z-bruker og her kan man filtrerer på `:SYK`


## Kontakt 
Du finner oss på slack `#flex`.
