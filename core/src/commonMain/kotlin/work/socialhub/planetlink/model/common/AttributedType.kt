package work.socialhub.planetlink.model.common

interface AttributedType {

    /** 属性の種類を取得  */
    val kind: AttributedKind

    /** 正規表現を取得  */
    val regex: Regex

    /** 表示文字列を取得  */
    fun displayedText(m: MatchResult): String

    /** 文字列を取得  */
    fun expandedText(m: MatchResult): String

    /**
     * AttributedType のデフォルト実装
     */
    class CommonAttributedType(
        override val kind: AttributedKind,
        override val regex: Regex,
        val display: ((MatchResult) -> String)?,
        val expand: ((MatchResult) -> String)?,
    ) : AttributedType {

        constructor(
            kind: AttributedKind,
            regex: Regex
        ) : this(
            kind,
            regex,
            { it.value },
            { it.value }
        )

        override fun displayedText(m: MatchResult): String {
            return display?.let { it(m) } ?: m.value
        }

        override fun expandedText(m: MatchResult): String {
            return expand?.let { it(m) } ?: m.value
        }
    }
}
