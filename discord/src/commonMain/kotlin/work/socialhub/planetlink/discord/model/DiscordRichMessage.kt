package work.socialhub.planetlink.discord.model

import kotlin.js.JsExport
import work.socialhub.planetlink.model.Media
import work.socialhub.planetlink.model.common.AttributedString

/** Discord rich embed retained alongside the unified comment fields. */
@JsExport
class DiscordEmbed {
    var title: String? = null
    var type: String? = null
    var description: String? = null
    var url: String? = null
    var timestamp: String? = null
    var color: Int? = null
    var footer: DiscordEmbedFooter? = null
    var image: DiscordMedia? = null
    var thumbnail: DiscordMedia? = null
    var video: DiscordMedia? = null
    var provider: DiscordEmbedProvider? = null
    var author: DiscordEmbedAuthor? = null
    var fields: List<DiscordEmbedField> = listOf()
    var contentScanVersion: Int? = null
}

@JsExport
class DiscordEmbedFooter {
    var text: String? = null
    var iconUrl: String? = null
    var proxyIconUrl: String? = null
}

@JsExport
class DiscordEmbedProvider {
    var name: String? = null
    var url: String? = null
}

@JsExport
class DiscordEmbedAuthor {
    var name: String? = null
    var url: String? = null
    var iconUrl: String? = null
    var proxyIconUrl: String? = null
}

@JsExport
class DiscordEmbedField {
    var name: String? = null
    var value: AttributedString? = null
    var inline: Boolean = false
}

/** Display-oriented representation of a Discord message component. */
@JsExport
class DiscordMessageComponent {
    var type: Int? = null
    var id: Int? = null
    var text: AttributedString? = null
    var url: String? = null
    var disabled: Boolean = false
    var spoiler: Boolean = false
    var medias: List<Media> = listOf()
    var children: List<DiscordMessageComponent> = listOf()
}

/** Discord-specific media metadata not represented by the unified Media model. */
@JsExport
class DiscordMedia : Media() {
    var contentType: String? = null
    var placeholder: String? = null
    var placeholderVersion: Int? = null
    var spoiler: Boolean = false
    var attachmentId: String? = null
}

@JsExport
class DiscordReactionDetails {
    var name: String? = null
    var emojiId: String? = null
    var normalCount: Int? = null
    var burstCount: Int? = null
    var burstColors: List<String> = listOf()
    var reactingNormally: Boolean = false
    var reactingWithBurst: Boolean = false
}

@JsExport
class DiscordUserPrimaryGuild {
    var identityGuildId: String? = null
    var identityEnabled: Boolean = false
    var tag: String? = null
    var badge: String? = null
}
