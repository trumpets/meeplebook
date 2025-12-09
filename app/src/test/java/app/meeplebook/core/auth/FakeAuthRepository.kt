package app.meeplebook.core.auth

import app.meeplebook.core.auth.model.AuthError
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
    var loginResult: AppResult<AuthCredentials, AuthError> = AppResult.Failure(AuthError.Unknown(IllegalStateException("FakeAuthRepository not configured")))

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

    override fun observeCurrentUser(): Flow<AuthCredentials?> = _currentUser

    override suspend fun getCurrentUser(): AuthCredentials? {
        return _currentUser.value
    }

    override suspend fun login(username: String, password: String): AppResult<AuthCredentials, AuthError> {
        loginCallCount++
        lastLoginUsername = username
        lastLoginPassword = password

        when (val result = loginResult) {
            is AppResult.Success -> _currentUser.value = result.data
            is AppResult.Failure -> { /* no-op */ }
        }

        return loginResult
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override fun isLoggedIn(): Flow<Boolean> = _currentUser.map { it != null }

    /**
     * Directly sets the current user for testing purposes.
     * This is useful for tests that need to simulate an authenticated state
     * without going through the full login flow.
     */
    fun setCurrentUser(credentials: AuthCredentials?) {
        _currentUser.value = credentials
    }
}
