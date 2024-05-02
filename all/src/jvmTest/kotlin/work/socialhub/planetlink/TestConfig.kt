package work.socialhub.planetlink

import kotlinx.serialization.Serializable
import work.socialhub.planetlink.config.BlueskyTestConfig
import work.socialhub.planetlink.config.MastodonTestConfig
import work.socialhub.planetlink.config.MisskeyTestConfig
import work.socialhub.planetlink.config.TumblrTestConfig

@Serializable
data class TestConfig(
    val bluesky: List<BlueskyTestConfig>,
    val misskey: List<MisskeyTestConfig>,
    val mastodon: List<MastodonTestConfig>,
    val tumblr: List<TumblrTestConfig>,
)