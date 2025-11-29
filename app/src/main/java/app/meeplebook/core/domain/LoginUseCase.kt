package app.meeplebook.core.domain

import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
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
     *         [Result.failure] with [IllegalArgumentException] if credentials are blank,
     *         or [Result.failure] with the underlying error if login fails
     */
    suspend operator fun invoke(username: String, password: String): Result<AuthCredentials> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username and password must not be blank"))
        }
        return authRepository.login(username, password)
    }
}
