package app.meeplebook.core.auth.remote

import app.meeplebook.core.model.AuthCredentials

/**
 * Fake implementation of [BggAuthRemoteDataSource] for testing purposes.
 */
class FakeBggAuthRemoteDataSource : BggAuthRemoteDataSource {

    /**
     * Configure this to control the result of [login] calls.
     * If null, login will throw the configured exception.
     */
    var loginResult: AuthCredentials? = null

    /**
     * Configure this exception to be thrown on [login] calls.
     */
    var loginException: Exception? = null

    /**
     * Tracks the number of times [login] was called.
     */
    var loginCallCount = 0
        private set

    /**
     * Stores the last username passed to [login].
     */
    var lastLoginUsername: String? = null
        private set

    /**
     * Stores the last password passed to [login].
     */
    var lastLoginPassword: String? = null
        private set

    override suspend fun login(username: String, password: String): AuthCredentials {
        loginCallCount++
        lastLoginUsername = username
        lastLoginPassword = password

        loginException?.let { throw it }
        return loginResult ?: throw IllegalStateException("FakeBggAuthRemoteDataSource not configured")
    }
}
