package no.nav.jfr.generell.operations.generell.norg;

import no.nav.jfr.generell.operations.eventenricher.pdl.ErrorMessage;

import java.util.List;

public class NorgErrorResponse {
    List<ErrorMessage> errors;

    NorgErrorResponse() {}

    public void setErrors(List<ErrorMessage> errors) {
        this.errors = errors;
    }

    public List<ErrorMessage> getErrors() {
        return errors;
    }
}
