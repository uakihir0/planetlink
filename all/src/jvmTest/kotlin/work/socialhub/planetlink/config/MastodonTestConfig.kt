package work.socialhub.planetlink.config

import kotlinx.serialization.Serializable

@Serializable
data class MastodonTestConfig(
    val host: String,
    val redirectUri: String,
    val clientId: String,
    val clientSecret: String,
    val userToken: String,
    val service: String,
)