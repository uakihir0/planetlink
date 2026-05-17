package work.socialhub.planetlink.nostr.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.model.Account

class NostrRequest(
    account: Account
) : RequestActionImpl(account)
