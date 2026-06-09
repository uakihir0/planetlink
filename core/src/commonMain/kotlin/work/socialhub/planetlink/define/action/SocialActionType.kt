package work.socialhub.planetlink.define.action

import kotlin.js.JsExport

@JsExport
enum class SocialActionType : ActionType {

    // Get Comment
    GetComment,
    GetContext,

    // Post Comment
    PostComment,
    DeleteComment,
    EditComment,
    LikeComment,
    UnlikeComment,
    ShareComment,
    UnShareComment,

    // Get Users
    GetUser,
    GetUserMe,

    // Post Users
    FollowUser,  //
    UnfollowUser,
    MuteUser,  //
    UnmuteUser,  //
    BlockUser,  //
    UnblockUser,  //
    GetRelationship,  //

    // Channels
    GetChannels,  //

    // Notification
    GetNotification,

    // Reaction
    ReactionComment,
    UnreactionComment,

    // Bookmarks
    GetUserBookmarks,
    RemoveBookmark,
}
