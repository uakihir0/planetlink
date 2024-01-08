package work.socialhub.planetlink.define

import work.socialhub.planetlink.model.error.SocialHubError

/**
 * Special Error for fur
 * 特殊対応を行う場合のエラーメッセージを追加
 */
enum class ErrorType(
    private val messageEn: String,
    private val messageJa: String
) : SocialHubError {

    RATE_LIMIT_EXCEEDED(
        "Rate limit exceeded, please try again in a moment.",
        "時間当たりのリクエストの上限に達しました。時間をおいて再度お試しください。"
    )
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
