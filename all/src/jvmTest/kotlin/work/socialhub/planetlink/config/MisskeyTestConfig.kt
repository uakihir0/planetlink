package work.socialhub.planetlink.config

import kotlinx.serialization.Serializable

@Serializable
data class MisskeyTestConfig(
    val host: String,
    val clientId: String,
    val clientSecret: String,
    val userToken: String,
    val ownedUserToken: String,
)
