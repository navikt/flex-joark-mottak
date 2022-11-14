# flex-joark-mottak

Applikasjonen tar over for [jfr-generell](https://github.com/navikt/jfr-generell) på journalposter med tema SYK. 
Beriker metadata og sender enten videre til manuell-oppretter eller forsøker automatisk journalføring.

## Testing i dev
Her kan man opprette journalpost [flex-testdata-generator](https://flex-testdata-generator.dev.nav.no/papir-dokument)
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

## Endre offsett
Kafka må være innstallert på maskinen: `brew install kafka`. Kan verifiseres med å kjøre kommando: `kafka-consumer-group`.

For å hente ut kafka secrets 

**1. Logg inn på GCP** og gi deg nødvendige tilganger i naisdevice (aiven dev eller aiven prod)
  ```
  gcloud auth login
  ```
**2. Sett namespace og context**
  ```
    kubens flex
    kubectx dev-gcp
  ```
**3. Hent keystore/truststore**
  ```
  sh getConfig.sh flex-joark-mottak-c6f84444b-cw6ck
  ```
**3. Skaler ned antall pods**
  ```
  kubectl scale --replicas=0 deployment/flex-joark-mottak
  ```
**4. Be om tilgang til aiven-prod i naisdevice**

**5. Sett kafka offset**

  Offsett kan settes til earliest `--to-earliest` eller `--to-datetime 2022-02-03T08:00:00.000`
  
  :exclamation: NB! Aiven tid er 2 timer forskjell fra vår tid. Sett timestamp minst to timer før det du tror du trenger!:exclamation:

  ```
  kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-prod-kafka-nav-prod.aivencloud.com:26484 --group flex.flex-joark-mottak --topic teamdokumenthandtering.aapen-dok-journalfoering --reset-offsets --to-earliest --dry-run
  ```

  - Finn gruppenavn `kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-<context>-kafka-nav-<context>.aivencloud.com:26484 --list`
  - Se current offset `kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-<context>-kafka-nav-<context>.aivencloud.com:26484 --describe --group <gruppenavn>`
  
  :exclamation: For faktisk å kjøre kallet må `--dry-run` byttes ut med `--execute` :exclamation:

**6. Skaler opp antall pods**
  ```
  kubectl scale --replicas=1 deployment/flex-joark-mottak
  ```
**7. Fjern keystore/trusstore fra egen maskin**
  ```
  rm -rf ~/.config/kafka
  ```


## Kontakt 
Du finner oss på slack `#flex`.
