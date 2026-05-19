package work.socialhub.planetlink.action

import kotlin.js.JsExport

@JsExport
interface ServiceAuth<T> {
    val accessor: T
}
