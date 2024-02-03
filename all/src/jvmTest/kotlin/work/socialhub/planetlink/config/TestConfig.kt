package work.socialhub.planetlink.config

import kotlinx.serialization.Serializable

@Serializable
data class TestConfig(
    val bluesky: List<BlueskyTestConfig>,
)