package no.nav.helse.flex.infrastructure.security

data class AzureAdResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val ext_expires_in: Int
)
