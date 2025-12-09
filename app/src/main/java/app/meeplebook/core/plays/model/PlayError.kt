package app.meeplebook.core.plays.model

import app.meeplebook.core.network.RetryException

/**
 * Represents errors that can occur during plays operations.
 */
sealed interface PlayError {
    /** Network error occurred (no connectivity, timeout, etc.) */
    data object NetworkError : PlayError

    /** Maximum retry attempts reached while waiting for plays */
    data class MaxRetriesExceeded(val exception: RetryException) : PlayError

    /** User is not logged in */
    data object NotLoggedIn : PlayError

    /** Unknown error */
    data class Unknown(val throwable: Throwable) : PlayError
}
