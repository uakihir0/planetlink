package work.socialhub.planetlink.slack.model

import work.socialhub.planetlink.model.Media

/**
 * Create a Media with Slack authorization header for private file access.
 */
fun slackMedia(token: String? = null): Media {
    return Media().also {
        if (!token.isNullOrBlank()) {
            it.requestHeader["Authorization"] = "Bearer $token"
        }
    }
}
