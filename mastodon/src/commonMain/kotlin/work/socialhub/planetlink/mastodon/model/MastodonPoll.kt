package work.socialhub.planetlink.mastodon.model

import work.socialhub.planetlink.model.Emoji
import work.socialhub.planetlink.model.Poll
import work.socialhub.planetlink.model.Service

class MastodonPoll(
    service: Service
) : Poll(service) {

    /** emojis which contains in option titles  */
    var emojis: List<Emoji>? = null
}
