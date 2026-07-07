package work.socialhub.planetlink.discord.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.discord.model.DiscordComment
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import kotlin.js.JsExport

@JsExport
class DiscordRequest(
    account: Account
) : RequestActionImpl(account) {

    override fun channelTimeLine(id: Identify): CommentsRequest {
        val request = getCommentsRequest(
            TimeLineActionType.ChannelTimeLine,
            { paging -> account.action.channelTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.ChannelTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
        request.commentFrom().addParam(DiscordComment.CHANNEL_KEY, id.id!!.value())
        return request
    }

    override fun messageTimeLine(id: Identify): CommentsRequest {
        val request = getCommentsRequest(
            TimeLineActionType.MessageTimeLine,
            { paging -> account.action.messageTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.MessageTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
        request.commentFrom()
            .addParam(DiscordComment.CHANNEL_KEY, id.id!!.value())
            .isMessage(true)
        return request
    }
}
