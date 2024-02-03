package work.socialhub.planetlink.config

import kotlinx.serialization.Serializable

@Serializable
data class BlueskyTestConfig(
    val apiHost: String,
    val streamHost: String,
    val identify: String,
    val password: String,
)