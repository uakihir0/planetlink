package work.socialhub.planetlink.model.error

import work.socialhub.planetlink.define.LanguageType
import kotlin.js.JsExport

@JsExport
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
    @JsExport.Ignore
    fun messageForUser(language: LanguageType): String
}
