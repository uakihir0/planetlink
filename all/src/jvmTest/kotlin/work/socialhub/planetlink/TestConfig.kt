package work.socialhub.planetlink

/**
 * Test configuration backed by a flat Map<String, String>.
 * Keys match the environment variable / GitHub Actions secret names.
 */
class TestConfig(
    private val props: MutableMap<String, String>
) {
    operator fun get(key: String): String? = props[key]
    operator fun set(key: String, value: String) { props[key] = value }

    fun toMap(): Map<String, String> = props.toMap()
}
