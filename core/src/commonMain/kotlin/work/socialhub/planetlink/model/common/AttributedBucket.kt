package work.socialhub.planetlink.model.common


class AttributedBucket : AttributedElement {

    override var kind = AttributedKind.OTHER

    override var visible: Boolean = true

    private val params = mutableMapOf<String, String>()

    private var children = mutableListOf<AttributedElement>()

    override val displayText: String
        get() {
            if (!visible) {
                return ""
            }
            return children
                .map { it.displayText }
                .joinToString { "" }
        }

    override val expandedText: String?
        get() = null
}
