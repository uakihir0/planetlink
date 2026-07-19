package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.matrix.action.MatrixAction
import kotlin.test.Test

/**
 * Exercises the Matrix-specific [MatrixAction.resolveMedia] helper: it takes the
 * raw mxc:// URI carried by [work.socialhub.planetlink.model.User.iconImageUrl]
 * and downloads the bytes via the (authenticated) media API.
 *
 * Live test — requires MATRIX_HOST/MATRIX_ACCESS_TOKEN in secrets.json and an
 * account whose profile has an avatar.
 */
class ResolveMediaTest {

    @Nested
    inner class Avatar : AbstractTest() {

        @Test
        fun testMatrixAvatar() = runTest {
            val account = matrix()
            val me = account.action.userMe()
            val mxc = me.iconImageUrl
            println("=== Resolve Matrix Media ===")
            println("  iconImageUrl > $mxc")

            if (mxc == null || !mxc.startsWith("mxc://")) {
                println("  (no mxc avatar to resolve)")
                return@runTest
            }

            val action = account.action as MatrixAction
            // Full download.
            val bytes = action.resolveMedia(mxc)
            println("  downloaded   > ${bytes.size} bytes")
            assert(bytes.isNotEmpty())

            // Scaled thumbnail.
            val thumb = action.resolveMedia(mxc, width = 96, height = 96)
            println("  thumbnail    > ${thumb.size} bytes")
            assert(thumb.isNotEmpty())
        }
    }
}
