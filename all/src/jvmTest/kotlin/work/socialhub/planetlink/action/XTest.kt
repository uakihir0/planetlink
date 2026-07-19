package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.PrintClass.dumpComments
import work.socialhub.planetlink.x.action.XAction
import work.socialhub.planetlink.x.model.XPaging

class XTest : AbstractTest() {

    @Test
    fun recommendedTimeLine() = runTest {
        val action = x().action as XAction
        dumpComments(action.recommendedTimeLine(XPaging(20)))
    }

    @Test
    fun trends() = runTest {
        val action = x().action as XAction
        action.trends().forEach {
            println("${it.name}: ${it.volume}")
        }
    }
}
