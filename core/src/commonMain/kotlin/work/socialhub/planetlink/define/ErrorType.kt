package work.socialhub.planetlink.define

import work.socialhub.planetlink.model.error.SocialHubError
import kotlin.js.JsExport

/**
 * Special Error for fur
 * 特殊対応を行う場合のエラーメッセージを追加
 */
@JsExport
enum class ErrorType(
    private val messageEn: String,
    private val messageJa: String
) : SocialHubError {

    RATE_LIMIT_EXCEEDED(
        "Rate limit exceeded, please try again in a moment.",
        "時間当たりのリクエストの上限に達しました。時間をおいて再度お試しください。"
    ),
    AUTH_FAILED(
        "Authentication failed. Please re-authenticate.",
        "認証に失敗しました。再認証してください。"
    ),
    NOT_FOUND(
        "The requested resource was not found.",
        "リクエストされたリソースが見つかりませんでした。"
    ),
    NETWORK_ERROR(
        "Network error. Please check your connection.",
        "ネットワークエラーです。接続を確認してください。"
    ),
    SERVER_ERROR(
        "Server error. Please try again later.",
        "サーバーエラーです。しばらくしてから再試行してください。"
    ),
    ;

    fun message(languageType: LanguageType): String {
        if (languageType === LanguageType.ENGLISH) {
            return messageEn
        }
        if (languageType === LanguageType.JAPANESE) {
            return messageJa
        }
        return messageEn
    }

    override fun messageForUser(): String = message(LanguageType.ENGLISH)
    override fun messageForUser(language: LanguageType) = message(language)
}
