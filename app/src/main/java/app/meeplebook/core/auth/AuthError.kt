package app.meeplebook.core.auth

/**
 * Sealed class representing authentication errors in the domain layer.
 * These provide a type-safe way to handle different error scenarios.
 */
sealed class AuthError {
    object InvalidCredentials : AuthError()
    object NetworkError : AuthError()
    object EmptyCredentials : AuthError()
    data class Unknown(val throwable: Throwable) : AuthError()
}