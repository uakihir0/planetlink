package net.socialhub.planetlink.model.common

class AttributedItem : AttributedElement {
    // region // Getter&Setter
    override var kind: AttributedKind? = null

    // endregion
    override var visible: Boolean = true

    /** 表示するテキスト  */
    override var displayText: String? = null
        get() {
            if (!visible) {
                return ""
            }
            return field
        }

    /** 実際に処理するテキスト  */
    override var expandedText: String? = null
        get() {
            if (field == null) {
                return displayText
            }
            return field
        }
}
