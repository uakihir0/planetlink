package work.socialhub.planetlink.refer

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx as PlanetLinkExBluesky
import work.socialhub.planetlink.mastodon.expand.PlanetLinkEx as PlanetLinkExMastodon
import work.socialhub.planetlink.misskey.expand.PlanetLinkEx as PlanetLinkExMisskey

object Reference {

    /**
     * Create references and include them in the compilation target
     * リファレンスを作成してコンパイル対象に含める
     */
    val references = listOf(
        PlanetLink,
        PlanetLinkExBluesky,
        PlanetLinkExMisskey,
        PlanetLinkExMastodon
    )
}