package work.socialhub.planetlink.model.common

class AttributedItem : AttributedElement {

    override var kind: AttributedKind = AttributedKind.OTHER

    override var visible: Boolean = true

    /** 表示するテキスト  */
    override var displayText: String = ""
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
