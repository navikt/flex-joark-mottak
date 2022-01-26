package no.nav.helse.flex

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
