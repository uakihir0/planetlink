package work.socialhub.planetlink.action.group

import net.socialhub.planetlink.model.group.CommentGroup
import net.socialhub.planetlink.model.group.UserGroup

interface AccountGroupAction {

    /**
     * Get All User's Information
     * グループのユーザー情報を取得
     */
    fun userMe(): UserGroup

    /**
     * Get Timeline Comments
     * タイムラインを取得
     */
    fun homeTimeLine(): CommentGroup
}
