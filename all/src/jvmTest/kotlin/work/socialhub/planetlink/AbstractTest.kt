package work.socialhub.planetlink

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx.bluesky
import work.socialhub.planetlink.mastodon.expand.PlanetLinkEx.mastodon
import work.socialhub.planetlink.misskey.expand.PlanetLinkEx.misskey
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.tumblr.expand.PlanetLinkEx.tumblr
import java.io.File
import kotlin.test.BeforeTest

open class AbstractTest {

    var config: TestConfig? = null

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        prettyPrint = true
        explicitNulls = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val secretsFile = File("../secrets.json")

    @BeforeTest
    fun setupTest() {
        try {
            val jsonStr = secretsFile.readText()
            val map = json.decodeFromString<Map<String, Map<String, String>>>(jsonStr)
            map["planetlink"]?.let {
                config = TestConfig(it.toMutableMap())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeProps() {
        val c = checkNotNull(config)
        try {
            val all = json.decodeFromString<MutableMap<String, Map<String, String>>>(
                secretsFile.readText()
            ).toMutableMap()
            all["planetlink"] = c.toMap()
            secretsFile.writeText(json.encodeToString(all))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun bluesky(): Account {
        val c = checkNotNull(config)
        return PlanetLink.bluesky(
            checkNotNull(c["BLUESKY_API_HOST"]),
            checkNotNull(c["BLUESKY_STREAM_HOST"]),
        ).accountWithIdentifyAndPassword(
            checkNotNull(c["BLUESKY_IDENTIFY"]),
            checkNotNull(c["BLUESKY_PASSWORD"]),
        )
    }

    fun misskey(): Account {
        val c = checkNotNull(config)
        return PlanetLink.misskey(
            checkNotNull(c["MISSKEY_HOST"]),
        ).accountWithAccessToken(
            checkNotNull(c["MISSKEY_USER_TOKEN"]),
        )
    }

    fun mastodon(): Account {
        val c = checkNotNull(config)
        return PlanetLink.mastodon(
            checkNotNull(c["MASTODON_HOST"]),
            c["MASTODON_SERVICE"] ?: "",
        ).accountWithAccessToken(
            checkNotNull(c["MASTODON_USER_TOKEN"]),
            null,
            null,
        )
    }

    fun tumblr(): Account {
        val c = checkNotNull(config)
        return PlanetLink.tumblr()
            .setConsumerInfo(
                checkNotNull(c["TUMBLR_CLIENT_ID"]),
                checkNotNull(c["TUMBLR_CLIENT_SECRET"]),
            )
            .setTokenRefreshCallback {
                println(">> Token Refreshed <<")
                c["TUMBLR_ACCESS_TOKEN"] = it.accessToken!!
                c["TUMBLR_REFRESH_TOKEN"] = it.refreshToken!!
                writeProps()
            }
            .accountWithAccessToken(
                checkNotNull(c["TUMBLR_ACCESS_TOKEN"]),
                checkNotNull(c["TUMBLR_REFRESH_TOKEN"]),
            )
    }

    fun icon(): ByteArray {
        return javaClass.getResourceAsStream("/icon.png").readAllBytes()
    }
}
