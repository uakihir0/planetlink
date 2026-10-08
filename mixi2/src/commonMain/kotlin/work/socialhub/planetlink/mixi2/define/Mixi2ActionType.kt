package work.socialhub.planetlink.mixi2.define

import kotlin.js.JsExport
import work.socialhub.planetlink.define.action.ActionType

/**
 * mixi2 固有のアクション種別
 * (共通のアクション種別で表せない mixi2 のタイムライン)
 */
@JsExport
enum class Mixi2ActionType : ActionType {

    /** おすすめタイムライン */
    RecommendedTimeLine,

    /** フォロー中タイムライン */
    FollowingTimeLine,

    /** ハッシュタグタイムライン */
    HashtagTimeLine,
}
