package app.meeplebook.core.auth.model

/**
 * Sealed class representing authentication errors in the domain layer.
 * These provide a type-safe way to handle different error scenarios.
 */
sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object NetworkError : AuthError
    data object EmptyCredentials : AuthError
    data class Unknown(val throwable: Throwable) : AuthError
}