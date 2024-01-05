package net.socialhub.planetlink.model.support

class Color {
    // region
    var r: Int = 0
    var g: Int = 0
    var b: Int = 0

    // endregion
    var a: Int = 0

    /**
     * Initialize with white color.
     */
    constructor() {
        r = 255
        g = 255
        b = 255
        a = 255
    }

    /**
     * Initialize with JavaScript expression.
     */
    constructor(javaScriptColorExpression: String?) {
        val p: java.util.regex.Pattern = java.util.regex.Pattern.compile("rgb\\(([0-9]+),([0-9]+),([0-9]+)\\)")
        val m: java.util.regex.Matcher = p.matcher(javaScriptColorExpression)

        if (m.find()) {
            r = m.group(1).toInt()
            g = m.group(2).toInt()
            b = m.group(3).toInt()
            a = 255
        }
    }

    /**
     * Get JavaScript Color Format
     * JavaScript で扱う色フォーマットに変換
     */
    fun toJavaScriptFormat(): String {
        return "rgb($r,$g,$b)"
    }
}