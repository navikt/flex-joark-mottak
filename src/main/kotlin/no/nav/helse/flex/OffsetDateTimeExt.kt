package no.nav.helse.flex

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

val osloZone = ZoneId.of("Europe/Oslo")

fun Instant.tilOsloZone(): OffsetDateTime = this.atZone(osloZone).toOffsetDateTime()
fun Instant.tilOsloLocalDateTime(): LocalDateTime = this.tilOsloZone().toLocalDateTime()
