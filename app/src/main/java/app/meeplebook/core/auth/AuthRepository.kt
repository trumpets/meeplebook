package app.meeplebook.core.auth

import app.meeplebook.core.model.AuthCredentials
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /** Emits the currently authenticated user, or null if logged out */
    fun currentUser(): Flow<AuthCredentials?>

    /** Performs a login with BGG */
    suspend fun login(username: String, password: String): Result<AuthCredentials>

    /** Logs out (clear token, cookies, user info). */
    suspend fun logout()

    /** Refreshes the user profile from remote and updates local storage */
    suspend fun refreshUser(): Result<Unit>

    /** Returns whether we hold a valid auth session */
    fun isLoggedIn(): Flow<Boolean>
}