package work.socialhub.planetlink.define.action

import kotlin.js.JsExport

@JsExport
enum class TimeLineActionType : ActionType {

    // TimeLine
    HomeTimeLine,
    MentionTimeLine,
    UserCommentTimeLine,
    UserLikeTimeLine,
    UserMediaTimeLine,
    SearchTimeLine,
    ChannelTimeLine,
    MessageTimeLine,
}
