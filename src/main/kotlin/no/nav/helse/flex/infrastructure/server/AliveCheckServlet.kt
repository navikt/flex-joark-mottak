package no.nav.helse.flex.infrastructure.server

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AliveCheckServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        resp.status = HttpServletResponse.SC_OK
        resp.writer.println("alive")
    }
}
