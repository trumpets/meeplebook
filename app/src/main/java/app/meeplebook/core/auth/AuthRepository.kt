package app.meeplebook.core.auth

import app.meeplebook.core.auth.model.AuthError
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits the currently authenticated user, or null if logged out */
    fun observeCurrentUser(): Flow<AuthCredentials?>

    /** Gets the currently authenticated user, or null if logged out */
    suspend fun getCurrentUser(): AuthCredentials?

    /** Performs a login with BGG */
    suspend fun login(username: String, password: String): AppResult<AuthCredentials, AuthError>

    /** Logs out (clear token, cookies, user info). */
    suspend fun logout()

    /** Returns whether we hold a valid auth session */
    fun isLoggedIn(): Flow<Boolean>
}