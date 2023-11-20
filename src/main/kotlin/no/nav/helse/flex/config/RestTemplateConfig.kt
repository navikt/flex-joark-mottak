package no.nav.helse.flex.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig {
    @Bean
    fun plainRestTemplate(
        restTemplateBuilder: RestTemplateBuilder
    ): RestTemplate =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(5L))
            .setReadTimeout(Duration.ofSeconds(10L))
            .build()
}
