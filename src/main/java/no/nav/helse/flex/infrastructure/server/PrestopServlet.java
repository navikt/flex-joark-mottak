package no.nav.helse.flex.infrastructure.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PrestopServlet extends HttpServlet {
    private static Logger log = LoggerFactory.getLogger(PrestopServlet.class);

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        try {
            log.info("Received pre-stop signal from Kubernetes - awaiting 5 seconds before allowing sigterm");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.warn("Prestop interrupted", e);
            Thread.currentThread().interrupt();
        }
        resp.setStatus(200);
    }
}

