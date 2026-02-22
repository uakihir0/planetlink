package work.socialhub.planetlink.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun <T> toBlocking(block: suspend CoroutineScope.() -> T): T {
    return runBlocking {
        block()
    }
}
