package work.socialhub.planetlink.utils

import io.ktor.http.*

object StringUtil {
    private const val MAX_DISPLAY_LENGTH = 26

    /**
     * URL デコード処理
     */
    fun String.decodeUrl(): String {
        return replace("&gt;".toRegex(), ">")
            .replace("&lt;".toRegex(), "<")
            .replace("&amp;".toRegex(), "&")
            .replace("&quot;".toRegex(), "\"")
    }

    /**
     * 文字の最後の空白を除去
     */
    fun String.trimLast(): String {
        return replace("\\s+$".toRegex(), "")
    }

    /**
     * 表示向け URL 文字列の作成
     */
    fun String.getDisplayUrl(): String {

        // プロトコルがある場合
        val url = removeProtocolUrl()
        if (url.length <= MAX_DISPLAY_LENGTH) {
            return url
        }

        return url.substring(0, MAX_DISPLAY_LENGTH) + "..."
    }

    /**
     * Http のプロトコル部分を除去
     */
    fun String.removeProtocolUrl(): String {

        // プロトコルがある場合
        if (startsWith("http")) {
            try {
                val url = Url(this)
                return (url.authority + url.fullPath)
            } catch (ignore: Exception) {
            }
        }
        return this
    }
}
