package work.socialhub.planetlink.model.common

import kotlin.test.Test
import kotlin.test.assertEquals

class AttributedBucketTest {

    @Test
    fun displayTextJoinsChildrenWithoutSeparator() {
        val bucket = AttributedBucket()
        bucket.add(AttributedItem().also { it.displayText = "hello" })
        bucket.add(AttributedItem().also { it.displayText = " world" })

        assertEquals("hello world", bucket.displayText)

        bucket.visible = false
        assertEquals("", bucket.displayText)
    }
}
