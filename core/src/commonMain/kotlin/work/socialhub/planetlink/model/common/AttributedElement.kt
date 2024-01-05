package net.socialhub.planetlink.model.common

/**
 * Attributes Elements
 * 属性情報
 */
interface AttributedElement {
    /** Get type of element.  */
    val kind: net.socialhub.planetlink.model.common.AttributedKind

    /** Get text that user see.  */
    val displayText: String

    /** Get text that user action.  */
    val expandedText: String?

    var visible: Boolean
}