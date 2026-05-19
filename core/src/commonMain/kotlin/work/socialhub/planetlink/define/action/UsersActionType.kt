package work.socialhub.planetlink.define.action

import kotlin.js.JsExport

@JsExport
enum class UsersActionType : ActionType {

    // Users
    GetFollowingUsers,
    GetFollowerUsers,
    SearchUsers,
    ChannelUsers,
}
