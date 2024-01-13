package work.socialhub.planetlink.model

import work.socialhub.planetlink.define.LanguageType
import work.socialhub.planetlink.model.error.SocialHubError

class Error(
    private val messageForUser: String
) : SocialHubError {

    /**
     * Error message map.
     * エラーメッセージ
     */
    private val messages = mutableMapOf<LanguageType, String>()

    /**
     * Set error message for specified language.
     * 特定の言語でのエラーメッセージを取得
     */
    fun addMessage(
        language: LanguageType,
        message: String,
    ) {
        messages[language] = message
    }

    /**
     * {@inheritDoc}
     */
    override fun messageForUser() =
        messageForUser

    /**
     * {@inheritDoc}
     */
    override fun messageForUser(language: LanguageType) =
        messages[language] ?: messageForUser
}
