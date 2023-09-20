package no.nav.helse.flex

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.retry.annotation.EnableRetry

@SpringBootApplication
@EnableRetry
@EnableCaching
@EnableJwtTokenValidation
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
