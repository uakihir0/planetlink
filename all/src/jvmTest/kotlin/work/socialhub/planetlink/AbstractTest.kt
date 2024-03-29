package work.socialhub.planetlink

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import work.socialhub.planetlink.bluesky.expand.PlanetLinkEx.bluesky
import work.socialhub.planetlink.config.TestConfig
import work.socialhub.planetlink.model.Account
import java.io.FileReader
import kotlin.test.BeforeTest

open class AbstractTest {

    var config: TestConfig? = null

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        explicitNulls = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @BeforeTest
    fun setupTest() {
        try {
            val string = readFile("../secrets.json")
            config = json.decodeFromString<TestConfig>(string!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Read File
     */
    private fun readFile(file: String): String {
        return FileReader(file).readText()
    }

    fun bluesky(index: Int = 0): Account {
        val c = checkNotNull(config).bluesky[index]
        return PlanetLink.bluesky(
            c.apiHost,
            c.streamHost
        ).accountWithIdentifyAndPassword(
            c.identify,
            c.password
        )
    }
}