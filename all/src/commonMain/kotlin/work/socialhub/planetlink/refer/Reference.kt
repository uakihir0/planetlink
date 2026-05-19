package work.socialhub.planetlink.refer

import work.socialhub.planetlink.PlanetLink
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx as PlanetLinkExBluesky
import work.socialhub.planetlink.misskey.expand.PlanetLinkEx as PlanetLinkExMisskey
import work.socialhub.planetlink.mastodon.expand.PlanetLinkEx as PlanetLinkExMastodon
import work.socialhub.planetlink.matrix.expand.PlanetLinkEx as PlanetLinkExMatrix
import kotlin.js.JsExport

@JsExport
object Reference {

    val references = listOf(
        PlanetLink,
        PlanetLinkExBluesky,
        PlanetLinkExMisskey,
        PlanetLinkExMastodon,
        PlanetLinkExMatrix,
    )
}