# flex-joark-mottak

Applikasjonen tar over for [jfr-generell](https://github.com/navikt/jfr-generell) på journalposter med tema SYK. 
Beriker metadata og sender enten videre til manuell-oppretter eller forsøker automatisk journalføring.

## Endre offsett :exclamation: ikke testet :exclamation:
Kafka må være innstalert på maskinen: `brew install kafka`. Kan verifiseres med å kjøre kommando: `kafka-consumer-group`.

**1. Logg inn på GCP** med kommando: `gcloud auth login` og gi deg nødvendige tilganger i naisdevice (aiven dev eller aiven prod)
  ```
  gcloud auth login
  ```
**2. Sett namespace og context** med kommando: `kubens flex` og `kubectx dev-gcp` (prod-gcp)
  ```
    kubens flex
    kubectx dev-gcp
  ```
**3. Hent keystore/truststore** med kommando `sh getConfig.sh <podnavn>` mens du står i rotmappen til applikasjonen.
  ```
  sh getConfig.sh flex-joark-mottak-c6f84444b-cw6ck
  ```
**3. Skaler ned antall pods** til 0. Dette kan gjøres med kommando `kubectl scale --replicas=<antall> deployment/<appnavn>`
  ```
  kubectl scale --replicas=0 deployment/flex-joark-mottak
  ```
**4. Sett offset** med kommando `kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-<context>-kafka-nav-<context>.aivencloud.com:26484 --group <gruppenavn> --topic <topic> --reset-offsets --to-datetime <YYYY-MM-DDTHH:mm:ss.sss> --dry-run`
Offsett kan også settes til earliest, da byttes `--to-datetime <YYYY-MM-DDTHH:mm:ss.sss>` ut med `--to-earliest`
  ```
  kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-prod-kafka-nav-prod.aivencloud.com:26484 --group flex.flex-joark-mottak --topic teamdokumenthandtering.aapen-dok-journalfoering --reset-offsets --to-earliest --dry-run
  ```
:exclamation: For faktisk å kjøre kallet må `--dry-run` byttes ut med `--execute` :exclamation:

**5. Skaler opp antall pods** til samme antall som tidligere med samme kommando: `kubectl scale --replicas=<antall> deployment/<appnavn>`
  ```
  kubectl scale --replicas=1 deployment/flex-joark-mottak
  ```
**6. Fjern keystore/trusstore** fra egen maskin med kommando`rm -rf ~/.config/kafka`
  ```
  rm -rf ~/.config/kafka
  ```
:exclamation: NB! Aiven tid er 2 timer forskjell fra vår tid. Sett timestamp minst to timer før det du tror du trenger!:exclamation:
- Finn gruppenavn `kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-<context>-kafka-nav-<context>.aivencloud.com:26484 --list`
- Se current offset `kafka-consumer-groups --command-config ~/.config/aiven.conf --bootstrap-server nav-<context>-kafka-nav-<context>.aivencloud.com:26484 --describe --group <gruppenavn>`


## Kontakt 
Du finner oss på slack `#flex`.
