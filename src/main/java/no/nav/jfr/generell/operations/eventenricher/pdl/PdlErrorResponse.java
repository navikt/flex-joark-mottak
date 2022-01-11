package no.nav.jfr.generell.operations.eventenricher.pdl;

import java.util.List;

public class PdlErrorResponse {
    List<ErrorMessage> errors;

    PdlErrorResponse() {}

    public void setErrors(List<ErrorMessage> errors) {
        this.errors = errors;
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }
}
