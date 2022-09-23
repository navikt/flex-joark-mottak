package no.nav.helse.flex.infrastructure.server

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory

class PrestopServlet : HttpServlet() {
    private val log = LoggerFactory.getLogger(PrestopServlet::class.java)

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        try {
            log.info("Received pre-stop signal from Kubernetes - awaiting 5 seconds before allowing sigterm")
            Thread.sleep(5000)
        } catch (e: InterruptedException) {
            log.warn("Prestop interrupted", e)
            Thread.currentThread().interrupt()
        }
        resp.status = 200
    }
}
