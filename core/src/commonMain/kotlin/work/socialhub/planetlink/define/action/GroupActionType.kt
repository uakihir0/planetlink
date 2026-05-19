package work.socialhub.planetlink.define.action

import kotlin.js.JsExport

/**
 * SNS グループアクション一覧
 * SNS Group Actions
 */
@JsExport
enum class GroupActionType {
    GetTimeLine,
    GetUserMe,
}
