package work.socialhub.planetlink.bluesky.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlueskyStreamCacheTest {

    @Test
    fun cacheMergesProfilesByDidWithoutRemovingOldEntries() {
        val cache = BlueskyStreamCache()
        val old = BlueskyStreamProfile("did:plc:old", handle = "old.test")
        cache.replace(arrayOf(old), isComplete = true)

        val refreshed = BlueskyStreamProfile("did:plc:old", handle = "new.test")
        val added = BlueskyStreamProfile("did:plc:added")
        cache.merge(listOf(refreshed, added))

        assertEquals(2, cache.profiles.size)
        assertEquals(
            "new.test",
            cache.profiles.single { it.did == "did:plc:old" }.handle,
        )
        assertTrue(cache.isComplete)
    }

    @Test
    fun completeCacheStopsWhenNewestPageOverlaps() {
        assertTrue(
            shouldStopFollowingSync(
                cacheIsComplete = true,
                cachedDids = setOf("did:plc:known"),
                fetchedDids = listOf("did:plc:new", "did:plc:known"),
            )
        )
    }

    @Test
    fun incompleteOrDisjointCacheContinues() {
        assertFalse(
            shouldStopFollowingSync(
                cacheIsComplete = false,
                cachedDids = setOf("did:plc:known"),
                fetchedDids = listOf("did:plc:known"),
            )
        )
        assertFalse(
            shouldStopFollowingSync(
                cacheIsComplete = true,
                cachedDids = setOf("did:plc:known"),
                fetchedDids = listOf("did:plc:new"),
            )
        )
    }
}
