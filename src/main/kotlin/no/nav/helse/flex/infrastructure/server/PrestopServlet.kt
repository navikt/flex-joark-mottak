package no.nav.helse.flex.infrastructure.server

import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
