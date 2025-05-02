package no.nav.helse.flex.graphql

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, String>,
)
