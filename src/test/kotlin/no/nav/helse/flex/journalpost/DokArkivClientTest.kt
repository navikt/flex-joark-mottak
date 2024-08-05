package no.nav.helse.flex.journalpost

import FellesTestOppsett
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import mock.DigitalSoknadPerson
import no.nav.helse.flex.CORRELATION_ID
import okhttp3.mockwebserver.MockResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import java.util.*
import java.util.concurrent.TimeUnit

class DokArkivClientTest : FellesTestOppsett() {
    @Autowired
    private lateinit var dokArkivClient: DokArkivClient

    @Autowired
    private lateinit var prometheusMeterRegistry: PrometheusMeterRegistry

    @BeforeEach
    fun `Disse legges til i header`() {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString())
    }

    @AfterEach
    fun opprydding() {
        MDC.remove(CORRELATION_ID)
    }

    @Test
    fun `update journalpost happycase`() {
        dokArkivClient.updateJournalpost(DigitalSoknadPerson.journalpost)
        prometheusMeterRegistry.meters.find {
            it.id.name == "http.client.requests" &&
                it.id.tags.contains(
                    Tag.of(
                        "uri",
                        "/rest/journalpostapi/v1/journalpost/{id}",
                    ),
                )
        } shouldNotBeEqualTo null
        dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo
            "PUT /rest/journalpostapi/v1/journalpost/${DigitalSoknadPerson.JOURNALPOST_ID} HTTP/1.1"
    }

    @Test
    fun `update journalpost returnerer 4xx`() {
        dokarkivMockWebserver.enqueue(MockResponse().setResponseCode(404))
        assertThrows<HttpClientErrorException> {
            dokArkivClient.updateJournalpost(DigitalSoknadPerson.journalpost)
        }
        dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo
            "PUT /rest/journalpostapi/v1/journalpost/${DigitalSoknadPerson.JOURNALPOST_ID} HTTP/1.1"
    }

    @Test
    fun `update journalpost returnerer 5xx`() {
        dokarkivMockWebserver.enqueue(MockResponse().setResponseCode(500))
        assertThrows<HttpServerErrorException> {
            dokArkivClient.updateJournalpost(DigitalSoknadPerson.journalpost)
        }
        dokarkivMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo
            "PUT /rest/journalpostapi/v1/journalpost/${DigitalSoknadPerson.JOURNALPOST_ID} HTTP/1.1"
    }
}
