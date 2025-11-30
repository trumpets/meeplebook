package app.meeplebook.core.domain

import app.meeplebook.core.auth.AuthError
import app.meeplebook.core.auth.AuthRepository
import app.meeplebook.core.model.AuthCredentials
import app.meeplebook.core.result.AppResult
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
     * @return [AppResult.Success] with [AuthCredentials] if login succeeds,
     *         [AppResult.Failure] with [AuthError] if validation or authentication fails
     */
    suspend operator fun invoke(username: String, password: String): AppResult<AuthCredentials, AuthError> {
        if (username.isBlank() || password.isBlank()) {
            return AppResult.Failure(AuthError.EmptyCredentials)
        }

        return authRepository.login(username, password)
    }
}
