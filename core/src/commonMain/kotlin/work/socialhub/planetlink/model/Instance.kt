package net.socialhub.planetlink.model

/**
 * インスタンス情報
 * Instance Info
 * (for distributed SNS)
 */
class Instance(service: Service) : java.io.Serializable {
    //region // Getter&Setter
    var name: String? = null

    var host: String? = null

    var description: String? = null

    var iconImageUrl: String? = null

    var usersCount: Long? = null

    var statusesCount: Long? = null

    var connectionCount: Long? = null

    private var service: Service

    init {
        this.service = service
    }

    fun getService(): Service {
        return service
    }

    fun setService(service: Service) {
        this.service = service
    } //endregion
}
