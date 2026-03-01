package work.socialhub.planetlink.slack.action

import work.socialhub.planetlink.action.RequestActionImpl
import work.socialhub.planetlink.action.SerializedRequest
import work.socialhub.planetlink.action.request.CommentsRequest
import work.socialhub.planetlink.action.request.CommentsRequestImpl
import work.socialhub.planetlink.define.action.TimeLineActionType
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.slack.model.SlackComment

class SlackRequest(
    account: Account
) : RequestActionImpl(account) {

    override fun homeTimeLine(): CommentsRequest {
        val action = account.action as? SlackAction
        val generalChannel = action?.getGeneralChannel() ?: ""

        val request = getCommentsRequest(
            TimeLineActionType.HomeTimeLine,
            { paging -> account.action.homeTimeLine(paging) },
            SerializedRequest(TimeLineActionType.HomeTimeLine)
        )
        request.commentFrom().addParam(SlackComment.CHANNEL_KEY, generalChannel)
        return request
    }

    override fun channelTimeLine(id: Identify): CommentsRequest {
        val request = getCommentsRequest(
            TimeLineActionType.ChannelTimeLine,
            { paging -> account.action.channelTimeLine(id, paging) },
            SerializedRequest(TimeLineActionType.ChannelTimeLine)
                .add("id", id.id!!.toSerializedString())
        )
        request.commentFrom().addParam(SlackComment.CHANNEL_KEY, id.id!!.value())
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
            .addParam(SlackComment.CHANNEL_KEY, id.id!!.value())
            .isMessage(true)
        return request
    }
}
