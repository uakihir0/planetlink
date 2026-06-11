package work.socialhub.planetlink.action.callback

import kotlin.js.JsExport

@JsExport
interface EventCallback {
    // KMP/JS IR compiler excludes interfaces from ES module exports when they have
    // no static members (see ExportModelToJsStatements.kt in the Kotlin compiler).
    // Adding companion object provides a static member, forcing stable-name export.
    // Alternative: compiler flag -Xenable-implementing-interfaces-from-typescript (experimental)
    companion object
}
