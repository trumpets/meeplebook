package app.meeplebook.core.auth

import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of [AuthRepository] for testing purposes.
 * Allows configuring login behavior through [loginResult].
 */
class FakeAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthCredentials?>(null)

    /**
     * Configure this to control the result of [login] calls.
     */
    var loginResult: AppResult<AuthCredentials, AuthError> = AppResult.Success(AuthCredentials("", ""))

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

    override fun currentUser(): Flow<AuthCredentials?> = _currentUser

    override suspend fun login(username: String, password: String): AppResult<AuthCredentials, AuthError> {
        loginCallCount++
        lastLoginUsername = username
        lastLoginPassword = password

        val result = loginResult
        if (result is AppResult.Success) {
            _currentUser.value = result.data
        }

        return loginResult
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isLoggedIn(): Flow<Boolean> = _currentUser.map { it != null }
}
