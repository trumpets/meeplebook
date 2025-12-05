package app.meeplebook.core.collection.model

/**
 * Sealed class representing collection fetching errors.
 */
sealed interface CollectionError {
    data object NetworkError : CollectionError
    data object NotLoggedIn : CollectionError
    data object Timeout : CollectionError
    data class Unknown(val throwable: Throwable) : CollectionError
}
