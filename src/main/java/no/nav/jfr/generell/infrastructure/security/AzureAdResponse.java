package no.nav.jfr.generell.infrastructure.security;

public class AzureAdResponse {

    public AzureAdResponse() {

    }

    private String access_token;
    private String token_type;
    private Integer expires_in;
    private Integer ext_expires_in;

    String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    public Integer getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(Integer expires_in) {
        this.expires_in = expires_in;
    }

    public Integer getExt_expires_in() {
        return ext_expires_in;
    }

    public void setExt_expires_in(Integer ext_expires_in) {
        this.ext_expires_in = ext_expires_in;
    }
}
