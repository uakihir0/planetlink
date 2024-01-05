package net.socialhub.planetlink.action.group

import net.socialhub.planetlink.model.group.CommentGroup
import net.socialhub.planetlink.model.group.UserGroup

interface AccountGroupAction {
    /**
     * Get All User's Information
     * グループのユーザー情報を取得
     */
    val userMe: UserGroup?

    /**
     * Get Timeline Comments
     * タイムラインを取得
     */
    val homeTimeLine: CommentGroup?
}
