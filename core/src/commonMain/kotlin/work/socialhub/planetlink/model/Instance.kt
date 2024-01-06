package work.socialhub.planetlink.model

import work.socialhub.planetlink.model.Service

/**
 * インスタンス情報
 * Instance Info
 * (for distributed SNS)
 */
class Instance(
    var service: Service
) {
    var name: String? = null
    var host: String? = null
    var description: String? = null
    var iconImageUrl: String? = null

    var usersCount: Long? = null
    var statusesCount: Long? = null
    var connectionCount: Long? = null
}
