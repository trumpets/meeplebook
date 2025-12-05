package app.meeplebook.core.collection.model

/**
 * Represents errors that can occur during collection operations.
 */
sealed interface CollectionError {
    /** Network error occurred (no connectivity, timeout, etc.) */
    data object NetworkError : CollectionError

    /** The server returned a rate limit error (5xx) */
    data object RateLimitError : CollectionError

    /** Maximum retry attempts reached while waiting for collection */
    data object MaxRetriesExceeded : CollectionError

    /** User is not logged in */
    data object NotLoggedIn : CollectionError

    /** Unknown error */
    data class Unknown(val throwable: Throwable) : CollectionError
}
