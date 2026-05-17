package work.socialhub.planetlink.matrix.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import work.socialhub.kmatrix.stream.MatrixStream as KmatrixStream
import work.socialhub.planetlink.model.Stream

class MatrixStream(
    private val matrixStream: KmatrixStream,
) : Stream {

    private var scope: CoroutineScope? = null
    private var _isOpened = false

    override val isOpened: Boolean
        get() = _isOpened

    override suspend fun open() {
        _isOpened = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope!!.launch {
            matrixStream.syncStream()
                .catch { /* ignore stream errors */ }
                .collect { /* sync data is processed via event callbacks */ }
        }
    }

    override fun close() {
        scope?.cancel()
        _isOpened = false
    }
}
