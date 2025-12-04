package app.meeplebook.core.plays.model

/**
 * Sealed class representing plays fetching errors.
 */
sealed interface PlaysError {
    data object NetworkError : PlaysError
    data object NotLoggedIn : PlaysError
    data class Unknown(val throwable: Throwable) : PlaysError
}
