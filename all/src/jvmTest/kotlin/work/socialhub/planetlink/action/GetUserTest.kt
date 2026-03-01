package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dump
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service
import kotlin.test.Test

class GetUserTest {

    @Nested
    inner class Me : AbstractTest() {
        private suspend fun Account.act() =
            dump(action.userMe())

        @Test
        fun testBluesky() = runTest { bluesky().act() }

        @Test
        fun testMisskey() = runTest { misskey().act() }

        @Test
        fun testMastodon() = runTest { mastodon().act() }

        @Test
        fun testTumblr() = runTest { tumblr().act() }
    }

    @Nested
    inner class FromUrl : AbstractTest() {
        private suspend fun Account.act(url: String) =
            dump(action.user(url))

        @Test
        fun testBluesky() = runTest {
            bluesky().act("https://bsky.app/profile/uakihir0.com")
        }

        @Test
        fun testMisskey() = runTest {
            misskey().act("https://misskey.io/@uakihir0")
        }

        @Test
        fun testMastodon() = runTest {
            mastodon().act("https://mastodon.social/@uakihir0")
        }

        @Test
        fun testTumblr() = runTest {
            tumblr().act("https://www.tumblr.com/uakihiro")
        }
    }

    @Nested
    inner class FromIdentity : AbstractTest() {
        private suspend fun Account.act(id: Any) = dump(
            action.user(Identify(Service(service.type, this), ID(id)))
        )

        @Test
        fun testBluesky() = runTest {
            bluesky().act("did:plc:bwdof2anluuf5wmfy2upgulw")
        }

        @Test
        fun testMisskey() = runTest { misskey().act("88473vqwpf") }

        @Test
        fun testMastodon() = runTest { mastodon().act("1223371") }

        @Test
        fun testTumblr() = runTest { tumblr().act("uakihiro") }
    }
}
