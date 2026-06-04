package work.socialhub.planetlink.matrix.action

import kotlin.js.JsExport
import work.socialhub.kmatrix.Matrix
import work.socialhub.kmatrix.MatrixFactory
import work.socialhub.kmatrix.api.request.login.LoginPasswordRequest
import work.socialhub.planetlink.action.ServiceAuth
import work.socialhub.planetlink.model.Account
import work.socialhub.planetlink.model.Service

@JsExport
class MatrixAuth(
    var host: String,
    var accessToken: String? = null,
) : ServiceAuth<Matrix> {

    private var _accessor: Matrix? = null

    override val accessor: Matrix
        get() = checkNotNull(_accessor) { "Matrix accessor is not initialized." }

    suspend fun accountWithPassword(
        user: String,
        password: String,
    ): Account {
        val tempMatrix = MatrixFactory.instance(host)
        val loginResponse = tempMatrix.login().loginWithPassword(
            LoginPasswordRequest().apply {
                this.user = user
                this.password = password
            }
        )
        val token = loginResponse.data.accessToken
        return accountWithAccessToken(token)
    }

    fun accountWithAccessToken(
        token: String,
    ): Account {
        accessToken = token
        val matrix = MatrixFactory.instance(host, token)
        this._accessor = matrix

        return Account().also { acc ->
            acc.action = MatrixAction(acc, this)
            acc.service = Service("matrix", acc).also {
                it.host = host
            }
        }
    }
}
