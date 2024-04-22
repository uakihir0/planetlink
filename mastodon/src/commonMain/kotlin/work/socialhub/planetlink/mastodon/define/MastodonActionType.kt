package work.socialhub.planetlink.mastodon.define

import work.socialhub.planetlink.define.action.ActionType

enum class MastodonActionType : ActionType {
    LocalTimeLine,
    FederationTimeLine,
}