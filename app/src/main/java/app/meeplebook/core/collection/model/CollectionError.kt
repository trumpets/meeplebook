package app.meeplebook.core.collection.model

import app.meeplebook.core.network.RetryException

/**
 * Represents errors that can occur during collection operations.
 */
sealed interface CollectionError {
    /** Network error occurred (no connectivity, timeout, etc.) */
    data object NetworkError : CollectionError

    /** Maximum retry attempts reached while waiting for collection */
    data class MaxRetriesExceeded(val exception: RetryException) : CollectionError

    /** User is not logged in */
    data object NotLoggedIn : CollectionError

    /** Unknown error */
    data class Unknown(val throwable: Throwable) : CollectionError
}
