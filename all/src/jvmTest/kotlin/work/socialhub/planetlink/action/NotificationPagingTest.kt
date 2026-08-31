package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.bluesky.action.BlueskyAction
import work.socialhub.planetlink.define.NotificationActionType
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationPagingTest : AbstractTest() {

    @Test
    fun testBlueskyNotificationPaging() = runTest {
        val account = bluesky()
        val action = account.action as BlueskyAction

        // 1ページ目: 5件取得
        val paging1 = Paging(5)
        val page1 = action.notification(paging1)
        println("=== Page 1 (${page1.entities.size} items) ===")
        page1.entities.forEach { n ->
            println("  ${n.type} | ${n.action} | ${n.createAt}")
        }

        // 2ページ目: pastPage で次ページを取得
        if (page1.entities.isNotEmpty()) {
            val paging2 = page1.pastPage()
            val page2 = action.notification(paging2)
            println("=== Page 2 (${page2.entities.size} items) ===")
            page2.entities.forEach { n ->
                println("  ${n.type} | ${n.action} | ${n.createAt}")
            }

            // 重複チェック
            val page1Ids = page1.entities.map { n -> n.id?.value }.toSet()
            val page2Ids = page2.entities.map { n -> n.id?.value }.toSet()
            val overlap = page1Ids.intersect(page2Ids)
            println("=== Overlap: ${overlap.size} items ===")
            if (overlap.isNotEmpty()) {
                println("  WARNING: Duplicate items found!")
                overlap.forEach { id -> println("    $id") }
            }

            // 3ページ目
            if (page2.entities.isNotEmpty()) {
                val paging3 = page2.pastPage()
                val page3 = action.notification(paging3)
                println("=== Page 3 (${page3.entities.size} items) ===")
                page3.entities.forEach { n ->
                    println("  ${n.type} | ${n.action} | ${n.createAt}")
                }

                val allIds = page1Ids + page2Ids
                val page3Ids = page3.entities.map { n -> n.id?.value }.toSet()
                val overlap2 = allIds.intersect(page3Ids)
                println("=== Overlap with previous pages: ${overlap2.size} items ===")
                if (overlap2.isNotEmpty()) {
                    println("  WARNING: Duplicate items found!")
                    overlap2.forEach { id -> println("    $id") }
                }
            }
        }
    }

    @Test
    fun testBlueskyNotificationActionTypes() = runTest {
        val account = bluesky()
        val action = account.action as BlueskyAction

        // メンション/返信のみを取得
        val mentions = action.notification(
            Paging(20),
            arrayOf(NotificationActionType.MENTION),
        )
        println("=== Mentions (${mentions.entities.size} items) ===")
        mentions.entities.forEach { n ->
            println("  ${n.type} | ${n.action} | ${n.comments?.size ?: 0} comments")
            assertEquals(NotificationActionType.MENTION.code, n.action)
            // 返信/メンションは通知自体が投稿を指すため対象投稿が引ける
            assertTrue(
                (n.comments?.size ?: 0) > 0,
                "a mention notification must carry its target post"
            )
        }

        // いいね/リポストのみを取得 (メンションが混ざらない)
        val reactions = action.notification(
            Paging(20),
            arrayOf(
                NotificationActionType.LIKE,
                NotificationActionType.SHARE,
            ),
        )
        println("=== Likes / Shares (${reactions.entities.size} items) ===")
        reactions.entities.forEach { n ->
            println("  ${n.type} | ${n.action}")
            assertTrue(
                n.action == NotificationActionType.LIKE.code ||
                        n.action == NotificationActionType.SHARE.code
            )
        }

        // 種別未指定は従来どおりメンションを含まない
        val legacy = action.notification(Paging(20))
        println("=== Legacy (${legacy.entities.size} items) ===")
        legacy.entities.forEach { n ->
            assertTrue(
                n.action != NotificationActionType.MENTION.code &&
                        n.action != NotificationActionType.QUOTE.code
            )
        }
    }
}
