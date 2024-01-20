package work.socialhub.planetlink.action

import kotlinx.serialization.Serializable

@Serializable
class SerializedRequest(
    var action: String
) {
    constructor(
        action: Enum<*>
    ) : this(action.name)

    val params = mutableMapOf<String, String>()

    fun add(key: String, value: String): SerializedRequest {
        params[key] = value
        return this
    }
}