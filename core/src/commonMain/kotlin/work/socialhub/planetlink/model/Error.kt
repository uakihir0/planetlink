package net.socialhub.planetlink.model

import net.socialhub.planetlink.define.LanguageType
import net.socialhub.planetlink.model.error.SocialHubError

class Error(
    /**
     * Default message.
     * デフォルトメッセージ
     */
    val messageForUser: String
) : SocialHubError {
    /**
     * Error message map.
     * エラーメッセージ
     */
    private val messages: MutableMap<LanguageType, String> = java.util.HashMap<LanguageType, String>()


    /**
     * Set error message for specified language.
     * 特定の言語でのエラーメッセージを取得
     */
    fun addMessage(language: LanguageType, message: String) {
        messages[language] = message
    }

    fun getMessageForUser(language: LanguageType): String {
        if (messages.containsKey(language)) {
            messages[language]
        }
        return messageForUser
    }
}
