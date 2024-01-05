package net.socialhub.planetlink.model.common.xml

import net.socialhub.planetlink.model.common.AttributedElement

/**
 * XML Element
 * [XmlElement]
 * [XmlString]
 */
interface XmlElement {
    fun setAttribute(
        elements: List<AttributedElement?>?,
        builder: java.lang.StringBuilder?,
        rule: XmlConvertRule?
    )
}
