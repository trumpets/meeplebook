package app.meeplebook.core.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import java.io.IOException
import javax.inject.Inject

/**
 * Use case that encapsulates the login logic.
 * Validates credentials and delegates to the [AuthRepository] for authentication.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    /**
     * Attempts to log in with the provided credentials.
     *
     * @param username The user's username
     * @param password The user's password
     * @return [Result.success] with [AuthCredentials] if login succeeds,
     *         [Result.failure] with [AuthError] if validation or authentication fails
     */
    suspend operator fun invoke(username: String, password: String): Result<AuthCredentials> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(AuthError.EmptyCredentials)
        }

        return authRepository.login(username, password).recoverCatching { throwable ->
            throw when (throwable) {
                is IOException -> AuthError.NetworkError
                is IllegalStateException -> AuthError.InvalidCredentials(throwable.message ?: "Invalid credentials")
                else -> AuthError.Unknown(throwable)
            }
        }
    }
}
