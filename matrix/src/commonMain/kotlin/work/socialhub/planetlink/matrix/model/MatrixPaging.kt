package work.socialhub.planetlink.matrix.model

import work.socialhub.planetlink.model.Paging

class MatrixPaging : Paging() {

    var from: String? = null
    var to: String? = null
    var direction: String? = null

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
