package no.nav.helse.flex.oppgave

import BaseTestClass
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.helse.flex.CORRELATION_ID
import no.nav.helse.flex.serialisertTilString
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

class OppgaveClientTest : BaseTestClass() {
    @Autowired
    private lateinit var oppgaveClient: OppgaveClient

    @Autowired
    private lateinit var prometheusMeterRegistry: PrometheusMeterRegistry

    val oppgaveRequest = OppgaveRequest(
        journalpostId = "x",
        aktoerId = "1919191919191",
        orgnr = "99999999",
        tema = "SYK",
        oppgavetype = "SOK",
        frist = 1,
        beskrivelse = "Test",
        tildeltEnhetsnr = "1234"
    )

    @BeforeEach
    fun `Disse legges til i header`() {
        MDC.put(CORRELATION_ID, UUID.randomUUID().toString())
    }

    @AfterEach
    fun opprydding() {
        MDC.remove(CORRELATION_ID)
    }

    @Test
    fun `opprett oppgave happycase`() {
        val request = oppgaveRequest.copy(journalpostId = "1")
        val oppgave = oppgaveClient.opprettOppgave(request)
        oppgave.tildeltEnhetsnr shouldBeEqualTo request.tildeltEnhetsnr

        prometheusMeterRegistry.meters.find { it.id.name == "http.client.requests" && it.id.tags.contains(Tag.of("uri", "/api/v1/oppgaver")) } shouldNotBeEqualTo null
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }

    @Test
    fun `opprett oppgave returnerer 400 med nedlagt enhet`() {
        val request = oppgaveRequest.copy(journalpostId = "2")
        oppgaveMockWebserver.enqueue(
            MockResponse().setResponseCode(400).setBody(
                OppgaveErrorResponse(
                    UUID.randomUUID().toString(),
                    "NAVEnheten 'xxxx' har status: 'Nedlagt'"
                ).serialisertTilString()
            )
        )
        val oppgave = oppgaveClient.opprettOppgave(request)
        oppgave.tildeltEnhetsnr shouldNotBeEqualTo request.tildeltEnhetsnr
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }

    @Test
    fun `opprett oppgave returnerer 400 med ugyldig orgnummer`() {
        val request = oppgaveRequest.copy(journalpostId = "3")
        oppgaveMockWebserver.enqueue(
            MockResponse().setResponseCode(400).setBody(
                OppgaveErrorResponse(
                    UUID.randomUUID().toString(),
                    "Organisasjonsnummer er ugyldig"
                ).serialisertTilString()
            )
        )
        oppgaveClient.opprettOppgave(request)
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }

    @Test
    fun `opprett oppgave returnerer 400 med uleselig response, kaster da http exception`() {
        val request = oppgaveRequest.copy(journalpostId = "4")
        oppgaveMockWebserver.enqueue(
            MockResponse().setResponseCode(400).setBody("Dette er en uleselig response")
        )
        assertThrows<HttpClientErrorException> {
            oppgaveClient.opprettOppgave(request)
        }
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }

    @Test
    fun `opprett oppgave returnerer 4xx`() {
        val request = oppgaveRequest.copy(journalpostId = "5")
        oppgaveMockWebserver.enqueue(MockResponse().setResponseCode(404))
        assertThrows<HttpClientErrorException> {
            oppgaveClient.opprettOppgave(request)
        }
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }

    @Test
    fun `opprett oppgave returnerer 5xx`() {
        val request = oppgaveRequest.copy(journalpostId = "6")
        oppgaveMockWebserver.enqueue(MockResponse().setResponseCode(500))
        assertThrows<HttpServerErrorException> {
            oppgaveClient.opprettOppgave(request)
        }
        oppgaveMockWebserver.takeRequest(1, TimeUnit.SECONDS)!!.requestLine shouldBeEqualTo "POST /api/v1/oppgaver HTTP/1.1"
    }
}
