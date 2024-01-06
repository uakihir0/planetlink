package work.socialhub.planetlink.model.common

/**
 * Attributes Elements
 * 属性情報
 */
interface AttributedElement {

    /** Get type of element.  */
    val kind: AttributedKind

    /** Get text that user see.  */
    val displayText: String

    /** Get text that user action.  */
    val expandedText: String?

    var visible: Boolean
}