package work.socialhub.planetlink.mastodon.define

import work.socialhub.planetlink.define.action.ActionType
import kotlin.js.JsExport

@JsExport
enum class MastodonActionType : ActionType {
    LocalTimeLine,
    FederationTimeLine,
    GetScheduledStatuses,
    GetScheduledStatus,
    PatchScheduledStatus,
    DeleteScheduledStatus,
    GetDomainBlocks,
    BlockDomain,
    UnblockDomain,
    PinComment,
    UnpinComment,
    ClearNotifications,
}