package net.socialhub.planetlink.model.request

class PollForm {
    private val options: MutableList<String> = java.util.ArrayList<String>()

    var multiple: Boolean? = null
        private set

    /** Expires in (min)  */
    var expiresIn: Long = 1440L
        private set

    fun addOption(option: String): PollForm {
        options.add(option)
        return this
    }

    fun multiple(multiple: Boolean?): PollForm {
        this.multiple = multiple
        return this
    }

    fun expiresIn(expiresIn: Long): PollForm {
        this.expiresIn = expiresIn
        return this
    }

    fun getOptions(): List<String> {
        return options
    }
}
