package work.socialhub.planetlink.bluesky.model

import kotlin.js.JsExport

/**
 * A persistable view of a followed account used by Bluesky streams.
 */
@JsExport
class BlueskyStreamProfile(
    var did: String,
    var handle: String? = null,
    var displayName: String? = null,
    var avatar: String? = null,
    var description: String? = null,
) {
    var followRecordUri: String? = null
    var followedRecordUri: String? = null
    var blockingRecordUri: String? = null
    var muted: Boolean? = null
    var blockedBy: Boolean? = null
}

/**
 * Externally owned cache of accounts followed by the authenticated user.
 *
 * Set [profiles] and [isComplete] from persistent storage before injecting
 * this instance into `BlueskyAuth.streamCache`. Stream setup merges newly
 * fetched profiles into this cache. Removed follows are intentionally retained.
 */
@JsExport
class BlueskyStreamCache {

    private val profilesByDid = mutableMapOf<String, BlueskyStreamProfile>()

    var profiles: Array<BlueskyStreamProfile>
        get() = profilesByDid.values.toTypedArray()
        set(value) {
            profilesByDid.clear()
            value.forEach { profilesByDid[it.did] = it }
        }

    /**
     * True when [profiles] came from a complete traversal of the follows list.
     */
    var isComplete: Boolean = false

    fun replace(
        profiles: Array<BlueskyStreamProfile>,
        isComplete: Boolean,
    ) {
        this.profiles = profiles
        this.isComplete = isComplete
    }

    fun clear() {
        profilesByDid.clear()
        isComplete = false
    }

    internal fun snapshot(): Map<String, BlueskyStreamProfile> =
        profilesByDid.toMap()

    internal fun merge(profiles: List<BlueskyStreamProfile>) {
        profiles.forEach { profilesByDid[it.did] = it }
    }
}

internal fun shouldStopFollowingSync(
    cacheIsComplete: Boolean,
    cachedDids: Set<String>,
    fetchedDids: List<String>,
): Boolean {
    return cacheIsComplete &&
        cachedDids.isNotEmpty() &&
        fetchedDids.any { it in cachedDids }
}
