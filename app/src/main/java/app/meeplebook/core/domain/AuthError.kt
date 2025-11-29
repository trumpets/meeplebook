package app.meeplebook.core.domain

/**
 * Sealed class representing authentication errors in the domain layer.
 * These provide a type-safe way to handle different error scenarios.
 */
sealed class AuthError : Throwable() {
    /** Credentials provided were invalid (wrong username/password) */
    data class InvalidCredentials(override val message: String) : AuthError()

    /** Network-related error (no connection, timeout, etc.) */
    data object NetworkError : AuthError()

    /** Username or password was blank/empty */
    data object EmptyCredentials : AuthError()

    /** Unknown or unexpected error */
    data class Unknown(val throwable: Throwable) : AuthError()
}
