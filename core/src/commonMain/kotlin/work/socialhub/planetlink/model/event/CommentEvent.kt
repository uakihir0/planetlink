package net.socialhub.planetlink.model.event


import work.socialhub.planetlink.model.Comment
import kotlin.js.JsExport

@JsExport
class CommentEvent(
    var comment: Comment
)
