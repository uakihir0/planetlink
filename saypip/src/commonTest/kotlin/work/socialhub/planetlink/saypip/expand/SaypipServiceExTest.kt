package work.socialhub.planetlink.saypip.expand

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.saypip.expand.ServiceEx.isSaypip

class SaypipServiceExTest {

    @Test
    fun identifiesASaypipService() {
        assertTrue(Service("saypip", Account()).isSaypip)
        assertTrue(Service("SAYPIP", Account()).isSaypip)
        assertFalse(Service("mastodon", Account()).isSaypip)
    }
}
