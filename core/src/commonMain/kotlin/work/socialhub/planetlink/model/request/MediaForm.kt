package net.socialhub.planetlink.model.request


class MediaForm {
    // ============================================================== //
    // Fields
    // ============================================================== //
    // ============================================================== //
    // Getters
    // ============================================================== //
    /** Media Data  */
    var data: ByteArray?

    /** Media File Name  */
    var name: String? = null

    /** Copy this object  */
    fun copy(): MediaForm {
        val model = MediaForm()
        model.data = data
        model.name = name
        return model
    }
}
