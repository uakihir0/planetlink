package work.socialhub.planetlink.action

import kotlinx.coroutines.test.runTest
import work.socialhub.planetlink.AbstractTest
import work.socialhub.planetlink.matrix.model.MatrixPaging
import work.socialhub.planetlink.matrix.model.MatrixSpace
import work.socialhub.planetlink.model.ID
import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Service
import kotlin.test.Test

/**
 * Live tests for the Matrix adapter. They require MATRIX_HOST plus either
 * MATRIX_ACCESS_TOKEN or MATRIX_USER/MATRIX_PASSWORD in secrets.json. Each test
 * skips gracefully when the host is not present.
 */
class MatrixTest : AbstractTest() {

    private fun hasHost(): Boolean =
        !config?.get("MATRIX_HOST").isNullOrBlank()

    @Test
    fun testSpaces() = runTest {
        if (!hasHost()) return@runTest
        // Single /sync; no per-room fetch.
        val spaces = matrix().action.spaces(MatrixPaging())
        spaces.entities.forEach {
            val space = it as MatrixSpace
            println("${space.roomId} > ${space.name} (${space.memberCount})")
        }
    }

    @Test
    fun testSpaceChannels() = runTest {
        if (!hasHost()) return@runTest
        val account = matrix()
        // 1st call fetches + caches the sync; 2nd call reuses the cache (no 2nd sync).
        val spaces = account.action.spaces(MatrixPaging())
        val space = spaces.entities.firstOrNull() ?: return@runTest
        val channels = account.action.channels(space, MatrixPaging())
        println("=== channels of ${space.name} ===")
        channels.entities.forEach { println("  ${it.id?.value<String>()} > ${it.name}") }
    }

    @Test
    fun testHomeChannels() = runTest {
        if (!hasHost()) return@runTest
        val account = matrix()
        // A non-space identify -> flat list of joined non-space rooms.
        val id = Identify(Service(account.service.type, account)).also {
            it.id = ID(account.action.userMe().id!!.value<String>())
        }
        val channels = account.action.channels(id, MatrixPaging())
        println("=== home channels (${channels.entities.size}) ===")
        channels.entities.forEach { println("  ${it.id?.value<String>()} > ${it.name}") }
    }

    @Test
    fun testMessageThread() = runTest {
        if (!hasHost()) return@runTest
        val threads = matrix().action.messageThread(MatrixPaging())
        println("=== DM threads (${threads.entities.size}) ===")
        threads.entities.forEach { println("  ${it.id?.value<String>()} > ${it.description}") }
    }
}
