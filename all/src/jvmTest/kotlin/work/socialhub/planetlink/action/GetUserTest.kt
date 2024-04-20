package work.socialhub.planetlink.action

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
        private fun Account.act() =
            dump(action.userMe())

        @Test
        fun testBluesky() = bluesky().act()

        @Test
        fun testMisskey() = misskey().act()

        @Test
        fun testMastodon() = mastodon().act()
    }

    @Nested
    inner class FromUrl : AbstractTest() {
        private fun Account.act(url: String) =
            dump(action.user(url))

        @Test
        fun testBluesky() = bluesky()
            .act("https://bsky.app/profile/uakihir0.com")

        @Test
        fun testMisskey() = misskey()
            .act("https://misskey.io/@uakihir0")
    }

    @Nested
    inner class FromIdentity : AbstractTest() {
        private fun Account.act(id: Any) = dump(
            action.user(Identify(Service(service.type, this), ID(id)))
        )

        @Test
        fun testBluesky() = bluesky().act("did:plc:bwdof2anluuf5wmfy2upgulw")

        @Test
        fun testMisskey() = misskey().act("88473vqwpf")
    }
}