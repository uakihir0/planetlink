package work.socialhub.planetlink.misskey.action

import kotlin.test.Test
import kotlin.test.assertEquals
import work.socialhub.kmisskey.api.response.i.IFavoritesResponse
import work.socialhub.kmisskey.entity.Note
import work.socialhub.kmisskey.entity.user.UserLite
import work.socialhub.planetlink.misskey.model.MisskeyComment
import work.socialhub.planetlink.misskey.model.MisskeyPaging
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

class MisskeyMapperTest {

    private val service = Service("misskey", Account())

    @Test
    fun favoritesTimeLine_preservesFavoriteOrderAndPagingIds() {
        val favorites = listOf(
            favorite(
                id = "favorite-new",
                noteId = "note-old",
                noteCreatedAt = "2025-01-01T00:00:00.000Z",
            ),
            favorite(
                id = "favorite-old",
                noteId = "note-new",
                noteCreatedAt = "2025-02-01T00:00:00.000Z",
            ),
        )

        val result = MisskeyMapper.favoritesTimeLine(
            favorites,
            "misskey.example",
            service,
            MisskeyPaging(),
        )
        val comments = result.entities.map { it as MisskeyComment }

        assertEquals(
            listOf("note-old", "note-new"),
            comments.map { it.id<String>() },
        )
        assertEquals(
            listOf("favorite-new", "favorite-old"),
            comments.map { it.idForPaging },
        )
        assertEquals(
            "favorite-old",
            (result.pastPage() as MisskeyPaging).untilId,
        )
    }

    private fun favorite(
        id: String,
        noteId: String,
        noteCreatedAt: String,
    ): IFavoritesResponse {
        return IFavoritesResponse().apply {
            this.id = id
            createdAt = "2025-03-01T00:00:00.000Z"
            this.noteId = noteId
            note = Note().apply {
                this.id = noteId
                createdAt = noteCreatedAt
                userId = "user"
                user = UserLite().apply {
                    this.id = "user"
                    username = "user"
                }
                visibility = "public"
            }
        }
    }
}
