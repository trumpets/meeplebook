package app.meeplebook.core.auth

import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of [AuthRepository] for testing purposes.
 * Allows configuring login behavior through [loginResult].
 */
class FakeAuthRepository : AuthRepository {

    private val _currentUser = MutableStateFlow<AuthCredentials?>(null)
    
    /**
     * Configure this to control the result of [login] calls.
     */
    var loginResult: Result<AuthCredentials> = Result.success(AuthCredentials("", ""))
    
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

    override suspend fun login(username: String, password: String): Result<AuthCredentials> {
        loginCallCount++
        lastLoginUsername = username
        lastLoginPassword = password
        
        loginResult.onSuccess { credentials ->
            _currentUser.value = credentials
        }
        
        return loginResult
    }

    override suspend fun logout() {
        _currentUser.value = null
    }

    override suspend fun refreshUser(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isLoggedIn(): Flow<Boolean> = MutableStateFlow(_currentUser.value != null)
}
