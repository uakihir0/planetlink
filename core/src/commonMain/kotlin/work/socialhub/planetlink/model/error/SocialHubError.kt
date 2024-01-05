package net.socialhub.planetlink.model.error

import net.socialhub.planetlink.define.LanguageType

interface SocialHubError {
    /**
     * Get error message with default language
     * デフォルト言語でエラーメッセージを取得
     */
    val messageForUser: String?

    /**
     * Get error message with specified language
     * 言語を指定してエラーメッセージを取得
     */
    fun getMessageForUser(language: LanguageType?): String?
}
