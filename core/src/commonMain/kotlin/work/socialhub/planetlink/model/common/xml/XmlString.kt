package net.socialhub.planetlink.model.common.xml

import net.socialhub.planetlink.model.common.AttributedElement

/**
 * XML String Element
 */
class XmlString : XmlElement {
    //endregion
    //region // Getter&Setter
    var string: String? = null

    override fun setAttribute(
        elements: List<AttributedElement?>?,
        builder: java.lang.StringBuilder,
        rule: XmlConvertRule?
    ) {
        builder.append(string)
    }
}
