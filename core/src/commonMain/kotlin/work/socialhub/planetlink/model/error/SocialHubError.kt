package work.socialhub.planetlink.model.error

import work.socialhub.planetlink.define.LanguageType

interface SocialHubError {

    /**
     * Get error message with default language
     * デフォルト言語でエラーメッセージを取得
     */
    fun messageForUser(): String

    /**
     * Get error message with specified language
     * 言語を指定してエラーメッセージを取得
     */
    fun messageForUser(language: LanguageType): String
}
