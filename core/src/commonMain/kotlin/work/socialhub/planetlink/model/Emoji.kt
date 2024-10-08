package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.emoji.EmojiType
import work.socialhub.planetlink.define.emoji.EmojiVariationType

/**
 * Emoji
 */
class Emoji {

    /**
     * 絵文字本体
     * (カスタム絵文字の場合はショートコードが入る)
     */
    var emoji: String? = null

    /**
     * 絵文字のショートコード
     * (:smile: など)
     */
    var shortCodes: List<String> = listOf()

    /**
     * カスタム絵文字の場合の画像 URL
     */
    var imageUrl: String? = null

    /**
     * 絵文字のカテゴリ
     */
    var category: String? = null

    /**
     * 絵文字の頻度
     * (カスタム絵文字の場合は 0 固定)
     */
    var frequentLevel: Int? = null

    fun addShortCode(shortCode: String) {
        shortCodes = (shortCodes + shortCode)
    }

    val shortCode: String
        get() = shortCodes[0]

    companion object {

        fun fromEmojiType(
            e: EmojiType
        ): Emoji {
            return Emoji().also {
                it.emoji = e.emoji
                it.addShortCode(e.name)
                it.category = e.category().code
                it.frequentLevel = e.level
            }
        }

        fun fromEmojiVariationType(
            e: EmojiVariationType
        ): Emoji {
            return Emoji().also {
                it.emoji = e.emoji
                it.addShortCode(e.name)
                it.category = e.category().code
                it.frequentLevel = e.level
            }
        }
    }
}
