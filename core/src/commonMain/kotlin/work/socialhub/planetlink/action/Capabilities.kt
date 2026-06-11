package work.socialhub.planetlink.action

import work.socialhub.planetlink.define.action.ActionType
import kotlin.js.JsExport

/**
 * Capabilities
 * アダプターがサポートする機能の一覧
 */
@JsExport
open class Capabilities(
    val supportedActions: Set<ActionType>
) {

    fun isSupported(action: ActionType): Boolean {
        return action in supportedActions
    }

    fun isAllSupported(actions: Array<ActionType>): Boolean {
        return actions.all { isSupported(it) }
    }

    fun isAnySupported(actions: Array<ActionType>): Boolean {
        return actions.any { isSupported(it) }
    }

    @JsExport.Ignore
    fun <T : ActionType> supportedOf(actions: Array<T>): List<T> {
        return actions.filter { isSupported(it) }
    }
}
