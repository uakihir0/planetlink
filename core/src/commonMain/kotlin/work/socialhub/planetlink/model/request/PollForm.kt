package work.socialhub.planetlink.model.request

class PollForm {

    val options = mutableListOf<String>()

    /** Multi Selection */
    var multiple: Boolean = false

    /** Expires in (min)  */
    var expiresIn: Int = 1440

    /**
     * Add poll option
     */
    fun addOption(option: String): PollForm {
        options.add(option)
        return this
    }

    /**
     * Set multiple
     */
    fun multiple(multiple: Boolean): PollForm {
        this.multiple = multiple
        return this
    }

    /**
     * Set expires in (min)
     */
    fun expiresIn(expiresIn: Int): PollForm {
        this.expiresIn = expiresIn
        return this
    }
}
