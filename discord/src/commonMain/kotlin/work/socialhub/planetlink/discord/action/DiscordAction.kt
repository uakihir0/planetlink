package work.socialhub.planetlink.discord.action

import work.socialhub.planetlink.action.AccountActionImpl
import work.socialhub.planetlink.model.Account
import kotlin.js.JsExport

/** Discord プラットフォームのアクション実装 */
@JsExport
class DiscordAction(
    account: Account,
    val auth: DiscordAuth,
) : AccountActionImpl(account)
