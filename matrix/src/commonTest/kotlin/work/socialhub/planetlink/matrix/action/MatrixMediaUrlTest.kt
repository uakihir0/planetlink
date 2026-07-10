package work.socialhub.planetlink.matrix.action

import work.socialhub.planetlink.matrix.model.MatrixSpace
import work.socialhub.planetlink.matrix.model.MatrixUser
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Offline tests for [MatrixMapper.mxcToHttpUrl]: mxc:// → unauthenticated v3
 * media URL an <img> can load directly. Pure string building — no network.
 */
class MatrixMediaUrlTest {

    private val base = "https://matrix.org"
    private val mxc = "mxc://matrix.org/NThAMmRZqMRqbyornNFgGLKN"

    private fun service(): Service {
        val svc = Service("matrix", Account())
        svc.host = base
        return svc
    }

    @Test
    fun testDownloadUrlWithoutSize() {
        val url = MatrixMapper.mxcToHttpUrl(mxc, base)
        assertEquals(
            "https://matrix.org/_matrix/media/v3/download/matrix.org/NThAMmRZqMRqbyornNFgGLKN?allow_redirect=true",
            url,
        )
    }

    @Test
    fun testThumbnailUrlWithSize() {
        val url = MatrixMapper.mxcToHttpUrl(mxc, base, 48, 48)
        assertEquals(
            "https://matrix.org/_matrix/media/v3/thumbnail/matrix.org/NThAMmRZqMRqbyornNFgGLKN" +
                "?width=48&height=48&method=scale&allow_redirect=true",
            url,
        )
    }

    @Test
    fun testMediaOnRemoteServerKeepsThatServer() {
        // The media's origin server comes from the mxc authority, not the
        // account's homeserver — the base host federates the request.
        val url = MatrixMapper.mxcToHttpUrl("mxc://other.example/abc123", base, 32, 32)
        assertEquals(
            "https://matrix.org/_matrix/media/v3/thumbnail/other.example/abc123" +
                "?width=32&height=32&method=scale&allow_redirect=true",
            url,
        )
    }

    @Test
    fun testTrailingSlashOnBaseIsNormalized() {
        val url = MatrixMapper.mxcToHttpUrl(mxc, "https://matrix.org/")
        assertEquals(
            "https://matrix.org/_matrix/media/v3/download/matrix.org/NThAMmRZqMRqbyornNFgGLKN?allow_redirect=true",
            url,
        )
    }

    @Test
    fun testNonMxcAndMalformedReturnNull() {
        assertNull(MatrixMapper.mxcToHttpUrl(null, base))
        assertNull(MatrixMapper.mxcToHttpUrl("", base))
        assertNull(MatrixMapper.mxcToHttpUrl("https://cdn.example/a.png", base))
        // Missing media id.
        assertNull(MatrixMapper.mxcToHttpUrl("mxc://matrix.org", base))
        assertNull(MatrixMapper.mxcToHttpUrl("mxc://matrix.org/", base))
        // Missing server.
        assertNull(MatrixMapper.mxcToHttpUrl("mxc:///abc", base))
        // Null / empty base uri (no homeserver on the service).
        assertNull(MatrixMapper.mxcToHttpUrl(mxc, null))
        assertNull(MatrixMapper.mxcToHttpUrl(mxc, ""))
    }

    @Test
    fun testUserMapperEmitsHttpNotMxc() {
        // The unified iconImageUrl the app reads must be an HTTP URL, never mxc.
        val user = MatrixMapper.user("@alice:matrix.org", "Alice", mxc, service()) as MatrixUser
        val icon = user.iconImageUrl
        assertTrue(icon != null && icon.startsWith("https://"), "icon should be http: $icon")
        assertTrue(!icon!!.startsWith("mxc://"), "icon must not be mxc: $icon")
        // The Matrix-specific field keeps the raw mxc for callers that want it.
        assertEquals(mxc, user.avatarUrl)
    }

    @Test
    fun testSpaceMapperEmitsHttpNotMxc() {
        val summary = MatrixRoomSummary(
            roomId = "!s:matrix.org",
            displayName = "Team",
            topic = null,
            avatarUrl = mxc,
            createAtMs = null,
            isSpace = true,
            isDirect = false,
            childRoomIds = emptyList(),
            memberCount = 3,
        )
        val space = MatrixMapper.space(summary, service()) as MatrixSpace
        val icon = space.iconUrl
        assertTrue(icon != null && icon.startsWith("https://"), "icon should be http: $icon")
    }
}
