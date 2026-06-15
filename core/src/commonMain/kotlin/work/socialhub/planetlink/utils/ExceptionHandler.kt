package work.socialhub.planetlink.utils

import io.ktor.utils.io.errors.IOException
import work.socialhub.planetlink.define.ErrorType
import work.socialhub.planetlink.model.error.*

object ExceptionHandler {

    fun classify(
        e: Exception,
        serviceName: String,
        statusCode: Int? = null,
        responseBody: String? = null,
    ): SocialHubException {
        if (e is SocialHubException) return e

        if (isNetworkError(e)) {
            return NetworkException(e.message, e).also {
                it.serviceName = serviceName
                it.error = ErrorType.NETWORK_ERROR
            }
        }

        if (statusCode != null) {
            return classifyHttpStatus(statusCode, responseBody, e, serviceName)
        }

        if (e is NullPointerException ||
            e is IllegalStateException ||
            e is IllegalArgumentException ||
            e is IndexOutOfBoundsException ||
            e is ClassCastException
        ) {
            return ApplicationException(e.message, e).also {
                it.serviceName = serviceName
            }
        }

        return SocialHubException(e.message, e).also {
            it.serviceName = serviceName
        }
    }

    fun classifyHttpStatus(
        statusCode: Int,
        responseBody: String?,
        cause: Throwable?,
        serviceName: String,
    ): SocialHubException {
        val exception: SocialHubException = when {
            statusCode == 401 || statusCode == 403 ->
                AuthException(statusCode, responseBody, cause).also {
                    it.error = ErrorType.AUTH_FAILED
                }
            statusCode == 404 ->
                NotFoundException(responseBody, cause).also {
                    it.error = ErrorType.NOT_FOUND
                }
            statusCode == 429 ->
                RateLimitException(responseBody, cause)
            statusCode in 400..499 ->
                ClientException(statusCode, responseBody, cause)
            statusCode in 500..599 ->
                ServerException(statusCode, responseBody, cause).also {
                    it.error = ErrorType.SERVER_ERROR
                }
            else ->
                HttpException(statusCode, responseBody, cause)
        }
        exception.serviceName = serviceName
        return exception
    }

    private fun isNetworkError(e: Throwable): Boolean {
        var current: Throwable? = e
        while (current != null) {
            if (current is IOException) return true
            val className = current::class.simpleName ?: ""
            if (className.contains("Timeout", ignoreCase = true) ||
                className.contains("ConnectException", ignoreCase = true) ||
                className.contains("UnresolvedAddress", ignoreCase = true)
            ) return true
            current = current.cause
        }
        return false
    }

    suspend fun <T> proceed(
        serviceName: String,
        statusExtractor: ((Exception) -> Int?)? = null,
        bodyExtractor: ((Exception) -> String?)? = null,
        runner: suspend () -> T,
    ): T {
        try {
            return runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw classify(
                e = e,
                serviceName = serviceName,
                statusCode = statusExtractor?.invoke(e),
                responseBody = bodyExtractor?.invoke(e),
            )
        }
    }

    suspend fun proceedUnit(
        serviceName: String,
        statusExtractor: ((Exception) -> Int?)? = null,
        bodyExtractor: ((Exception) -> String?)? = null,
        runner: suspend () -> Unit,
    ) {
        try {
            runner()
        } catch (e: SocialHubException) {
            throw e
        } catch (e: Exception) {
            throw classify(
                e = e,
                serviceName = serviceName,
                statusCode = statusExtractor?.invoke(e),
                responseBody = bodyExtractor?.invoke(e),
            )
        }
    }
}
