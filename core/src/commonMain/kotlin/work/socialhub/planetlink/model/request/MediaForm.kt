package work.socialhub.planetlink.model.request


class MediaForm(
    /** Media Data  */
    var data: ByteArray,
    /** Media File Name  */
    var name: String,
) {
    /** Copy this object  */
    fun copy(): MediaForm {
        return MediaForm(data, name)
    }
}
