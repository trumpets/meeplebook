package app.meeplebook.core.result

/**
 * A type-safe result wrapper that represents either success with data of type [T]
 * or failure with an error of type [E].
 *
 * @param T The type of data returned on success.
 * @param E The type of error returned on failure.
 */
sealed interface AppResult<out T, out E> {
    /**
     * Represents a successful result containing [data].
     *
     * @param data The data returned on success.
     */
    data class Success<T>(val data: T) : AppResult<T, Nothing>

    /**
     * Represents a failed result containing an [error].
     *
     * @param error The error returned on failure.
     */
    data class Failure<E>(val error: E) : AppResult<Nothing, E>
}