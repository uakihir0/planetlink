package work.socialhub.planetlink.model.common

import kotlin.js.JsExport

/**
 * Attributes Elements
 * 属性情報
 */
@JsExport
interface AttributedElement {

    /** Type of element */
    val kind: AttributedKind

    /** Text that user see */
    val displayText: String

    /** Text that user action */
    val expandedText: String?

    /** Visibility */
    var visible: Boolean
}