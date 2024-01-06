package work.socialhub.planetlink.define.action

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
}
