package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.bluesky.action.BlueskyAction
import work.socialhub.planetlink.model.Paging
import kotlin.test.Test

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
}
