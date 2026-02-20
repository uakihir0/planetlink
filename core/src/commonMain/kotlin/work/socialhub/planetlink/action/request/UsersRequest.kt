package work.socialhub.planetlink.action.request

import work.socialhub.planetlink.model.Pageable
import work.socialhub.planetlink.model.Paging
import work.socialhub.planetlink.model.Request
import work.socialhub.planetlink.model.User

interface UsersRequest : Request {

    /**
     * Get Users
     * ユーザーを取得
     */
    suspend fun users(paging: Paging): Pageable<User>
}
