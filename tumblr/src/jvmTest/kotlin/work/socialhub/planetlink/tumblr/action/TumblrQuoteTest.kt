package work.socialhub.planetlink.tumblr.action

import kotlinx.serialization.json.Json
import work.socialhub.ktumblr.entity.post.legacy.LegacyTextPost
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.request.MediaForm
import work.socialhub.planetlink.model.Service
import work.socialhub.planetlink.model.error.NotSupportedException
import work.socialhub.planetlink.model.request.CommentForm
import work.socialhub.planetlink.tumblr.define.TumblrFormKey
import work.socialhub.planetlink.tumblr.model.TumblrComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TumblrQuoteTest {

    private val service = Service("tumblr", Account())

    private fun comment() = TumblrComment(service).also {
        it.id = ID("123456789")
        it.reblogKey = "abcDEF"
    }

    @Test
    fun quoteFormCarriesReblogKey() {
        val form = comment().quoteForm
        assertEquals("123456789", form.quoteId?.value<String>())
        assertEquals("abcDEF", form.params[TumblrFormKey.REBLOG_KEY])
        assertEquals(false, form.isMessage)
    }

    @Test
    fun reblogRequestFromQuoteForm() {
        val form = comment().quoteForm.text("my comment")
        val req = TumblrAction.reblogRequest(form, "myblog.tumblr.com")

        assertEquals("myblog.tumblr.com", req.blogName)
        assertEquals("123456789", req.id)
        assertEquals("abcDEF", req.reblogKey)
        assertEquals("my comment", req.comment)

        val map = req.toMap()
        assertEquals("123456789", map["id"])
        assertEquals("abcDEF", map["reblog_key"])
        assertEquals("my comment", map["comment"])
    }

    @Test
    fun reblogRequestWithoutText() {
        val form = comment().quoteForm.text("")
        val req = TumblrAction.reblogRequest(form, "myblog.tumblr.com")
        assertNull(req.comment)
    }

    @Test
    fun reblogRequestRequiresReblogKey() {
        val form = CommentForm().quoteId(ID("123456789")).text("x")
        assertFailsWith<NotSupportedException> {
            TumblrAction.reblogRequest(form, "myblog.tumblr.com")
        }
    }

    @Test
    fun reblogRequestRejectsImages() {
        val form = comment().quoteForm.text("x")
        form.addImage(MediaForm(byteArrayOf(1, 2, 3), "a.png"))
        assertFailsWith<NotSupportedException> {
            TumblrAction.reblogRequest(form, "myblog.tumblr.com")
        }
    }

    @Test
    fun quoteOfReblogTargetsTheReblogWithItsKey() {
        // A reblog (id 222, key "reblogKey") of a root post (id 111).
        val post = Json { ignoreUnknownKeys = true; explicitNulls = false }.decodeFromString(
            LegacyTextPost.serializer(),
            """
            {
              "id_string": "222",
              "reblog_key": "reblogKey",
              "timestamp": 0,
              "parent_post_url": "https://parent.tumblr.com/post/150",
              "reblogged_root_id": "111",
              "reblogged_root_url": "https://root.tumblr.com/post/111",
              "reblogged_root_name": "root",
              "body": "<p>hello</p>"
            }
            """.trimIndent()
        )
        val shared = TumblrMapper.reblogComment(post, mapOf(), service) as TumblrComment

        // The shared comment shows the root post, but its key is the reblog's.
        assertEquals("111", shared.id<String>())
        assertEquals("222", shared.reblogKeyPostId)

        val form = shared.quoteForm.text("x")
        val req = TumblrAction.reblogRequest(form, "myblog.tumblr.com")
        assertEquals("222", req.id)
        assertEquals("reblogKey", req.reblogKey)
    }
}
