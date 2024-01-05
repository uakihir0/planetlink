package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.emoji.EmojiType
import net.socialhub.planetlink.define.emoji.EmojiVariationType

/**
 * Emoji
 */
class Emoji {
    // region // Getter&Setter
    /**
     * 絵文字本体
     * (カスタム絵文字の場合はショートコードが入る)
     */
    var emoji: String? = null

    /**
     * 絵文字のショートコード
     * (:smile: など)
     */
    var shortCodes: MutableList<String>? = null
        get() {
            if (field == null) {
                field = java.util.ArrayList<String>()
            }
            return field
        }

    /**
     * カスタム絵文字の場合の画像 URL
     */
    var imageUrl: String? = null

    /**
     * 絵文字のカテゴリ
     */
    var category: String? = null

    // endregion
    /**
     * 絵文字の頻度
     * (カスタム絵文字の場合は 0 固定)
     */
    var frequentLevel: Int? = null

    fun addShortCode(shortCode: String) {
        shortCodes!!.add(shortCode)
    }

    val shortCode: String
        get() = shortCodes!![0]

    companion object {
        fun fromEmojiType(e: EmojiType): Emoji {
            val emoji = Emoji()
            emoji.emoji = e.getEmoji()
            emoji.addShortCode(e.getName())
            emoji.category = e.getCategory().getCode()
            emoji.frequentLevel = e.getLevel()
            return emoji
        }

        fun fromEmojiVariationType(e: EmojiVariationType): Emoji {
            val emoji = Emoji()
            emoji.emoji = e.getEmoji()
            emoji.addShortCode(e.getName())
            emoji.category = e.getCategory().getCode()
            emoji.frequentLevel = e.getLevel()
            return emoji
        }
    }
}
