package work.socialhub.planetlink.matrix.model

import work.socialhub.planetlink.model.Identify
import work.socialhub.planetlink.model.Paging
import kotlin.js.JsExport

@JsExport
class MatrixPaging : Paging() {

    var from: String? = null
    var to: String? = null
    var direction: String? = null

    override fun <T : Identify> newPage(entities: List<T>): Paging {
        return copy().also {
            if (entities.isNotEmpty()) {
                // `from` is the token at the newest edge of the current
                // response. Use it with forward pagination to fetch newer
                // events.
                it.from = from
                it.to = null
                it.direction = "f"
            }
        }
    }

    override fun <T : Identify> pastPage(entities: List<T>): Paging {
        return copy().also {
            if (entities.isNotEmpty()) {
                // Matrix returns `end` as the token for the next chunk in the
                // requested direction. With backward pagination this is the
                // next older page.
                it.from = to
                it.to = null
                it.direction = "b"
            }
        }
    }

    override fun setMarkPagingEnd(entities: List<*>) {
        if (entities.isEmpty()) {
            when (direction) {
                "f" -> isHasNew = false
                "b" -> isHasPast = false
                else -> {
                    isHasNew = false
                    isHasPast = false
                }
            }
        } else if (to == null) {
            when (direction) {
                "f" -> isHasNew = false
                "b" -> isHasPast = false
            }
        }
    }

    override fun copy(): MatrixPaging {
        val p = MatrixPaging()
        copyTo(p)
        p.from = from
        p.to = to
        p.direction = direction
        return p
    }

    companion object {
        fun fromPaging(paging: Paging?): MatrixPaging {
            val p = MatrixPaging()
            if (paging != null) {
                paging.copyTo(p)
                if (paging is MatrixPaging) {
                    p.from = paging.from
                    p.to = paging.to
                    p.direction = paging.direction
                }
            }
            return p
        }
    }
}
