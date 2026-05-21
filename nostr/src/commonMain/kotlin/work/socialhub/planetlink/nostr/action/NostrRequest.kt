package work.socialhub.planetlink.nostr.action

import kotlin.js.JsExport
import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.model.Account

@JsExport
class NostrRequest(
    account: Account
) : RequestActionImpl(account)
