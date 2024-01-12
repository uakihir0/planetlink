package work.socialhub.planetlink.model.support

/**
 * Color Model
 * Initialize with white color.
 */
class Color(
    var r: Int = 255,
    var g: Int = 255,
    var b: Int = 255,
    var a: Int = 255,
) {

    companion object {
        /**
         * Initialize with JavaScript expression.
         */
        fun fromJavaScriptFormat(
            expression: String
        ): Color {
            val regex = "rgb\\(([0-9]+),([0-9]+),([0-9]+)\\)".toRegex()
            regex.find(expression)?.let {
                return Color(
                    it.groupValues[1].toInt(),
                    it.groupValues[2].toInt(),
                    it.groupValues[3].toInt(),
                )
            }
            throw IllegalArgumentException("Invalid color expression.")
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