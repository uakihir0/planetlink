package work.socialhub.planetlink.utils

import kotlinx.coroutines.CoroutineScope

expect fun <T> toBlocking(block: suspend CoroutineScope.() -> T): T
