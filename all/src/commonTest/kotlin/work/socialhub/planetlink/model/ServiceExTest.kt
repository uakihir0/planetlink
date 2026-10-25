package work.socialhub.planetlink.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceExTest {

    @Test
    fun isTumblrAcceptsBothSpellings() {
        val account = Account()
        assertTrue(ServiceEx.run { Service("tumblr", account).isTumblr })
        // Tumblr アダプタは "tumber" という綴りで Service を作る。
        assertTrue(ServiceEx.run { Service("tumber", account).isTumblr })
        assertFalse(ServiceEx.run { Service("mastodon", account).isTumblr })
    }
}
