package net.socialhub.planetlink.model.common

class AttributedBucket : AttributedElement {
    // region // Getter&Setter
    override var kind: AttributedKind? = null

    override var visible: Boolean = true

    private val params: MutableMap<String, String> = java.util.HashMap<String, String>()

    private var children: MutableList<AttributedElement> = java.util.ArrayList<AttributedElement>()

    override val displayText: String
        get() {
            if (!visible) {
                return ""
            }
            return children.stream()
                .map<Any>(AttributedElement::getDisplayText)
                .collect(java.util.stream.Collectors.joining())
        }

    override val expandedText: String?
        get() = null

    fun getParam(key: String): String? {
        return params[key]
    }

    fun addParam(key: String, value: String) {
        params[key] = value
    }

    fun getChildren(): MutableList<AttributedElement> {
        return children
    }

    fun setChildren(children: MutableList<AttributedElement>) {
        this.children = children
    } // endregion
}
