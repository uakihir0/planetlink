package work.socialhub.planetlink.matrix.action

import kotlin.js.JsExport
import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.model.Account

@JsExport
class MatrixRequest(
    account: Account
) : RequestActionImpl(account)
