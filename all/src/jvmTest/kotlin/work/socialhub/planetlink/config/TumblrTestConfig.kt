package work.socialhub.planetlink.config

import kotlinx.serialization.Serializable

@Serializable
data class TumblrTestConfig(
    var clientId: String,
    var clientSecret: String,
    var accessToken: String,
    var refreshToken: String,
)