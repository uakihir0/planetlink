package net.socialhub.planetlink.model.common

interface AttributedType {
    /** 属性の種類を取得  */
    val kind: net.socialhub.planetlink.model.common.AttributedKind?

    /** 正規表現を取得  */
    val regex: String?

    /** 表示文字列を取得  */
    fun getDisplayedText(m: java.util.regex.Matcher?): String?

    /** 文字列を取得  */
    fun getExpandedText(m: java.util.regex.Matcher?): String?

    /**
     * AttributedType のデフォルト実装
     */
    class CommonAttributedType(
        kind: AttributedKind,
        override val regex: String,
        display: java.util.function.Function<java.util.regex.Matcher?, String?>,
        expand: java.util.function.Function<java.util.regex.Matcher?, String?>
    ) : AttributedType {
        override val kind: AttributedKind = kind

        private val display: java.util.function.Function<java.util.regex.Matcher, String>? = display

        private val expand: java.util.function.Function<java.util.regex.Matcher, String>? = expand

        constructor(
            kind: AttributedKind,
            regex: String
        ) : this(kind, regex,
            java.util.function.Function<java.util.regex.Matcher, String> { obj: java.util.regex.Matcher -> obj.group() },
            java.util.function.Function<java.util.regex.Matcher, String> { obj: java.util.regex.Matcher -> obj.group() })

        override fun getKind(): AttributedKind {
            return kind
        }

        override fun getDisplayedText(m: java.util.regex.Matcher): String {
            if (display != null) {
                return display.apply(m)
            }
            // 未定義の場合は全体
            return m.group()
        }

        override fun getExpandedText(m: java.util.regex.Matcher?): String? {
            if (expand != null) {
                return expand.apply(m)
            }
            // 未定義
            return null
        }
    }
}
