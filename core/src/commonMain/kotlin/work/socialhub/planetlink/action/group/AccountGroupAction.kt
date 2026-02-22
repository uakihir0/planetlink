package work.socialhub.planetlink.action.group

import work.socialhub.planetlink.model.group.CommentGroup
import work.socialhub.planetlink.model.group.UserGroup

interface AccountGroupAction {

    /**
     * Get All User's Information
     * グループのユーザー情報を取得
     */
    suspend fun userMe(): UserGroup

    /**
     * Get Timeline Comments
     * タイムラインを取得
     */
    suspend fun homeTimeLine(): CommentGroup
}
