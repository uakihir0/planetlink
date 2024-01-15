package work.socialhub.planetlink.bluesky.support

import work.socialhub.kbsky.api.entity.share.AuthRequest
import work.socialhub.kbsky.internal.share._InternalUtility
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Utils {

    fun userIdentifyFromUrl(
        url: String
    ): String {
        val elements = url
            .split("/")
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        return elements[elements.size - 1]
    }

    fun userRkeyFromUrl(
        url: String
    ): String {
        val elements = url
            .split("//")
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
            .split("/")
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        return elements[4]
    }

    fun userHandleFromUrl(
        url: String
    ): String {
        val elements = url
            .split("//")
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
            .split("/")
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        return elements[2]
    }

    fun jwt(
        jwt: String
    ): AuthRequest.Jwt {
        val encodedJson = jwt
            .split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]

        @OptIn(ExperimentalEncodingApi::class)
        val decodedJson = Base64.decode(encodedJson)
        return _InternalUtility.fromJson(decodedJson.decodeToString())
    }
}