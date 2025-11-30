package app.meeplebook.core.result

/**
 * Transforms the [AppResult] into a value of type [R] by applying [onSuccess] if this is
 * a [AppResult.Success] or [onFailure] if this is a [AppResult.Failure].
 *
 * @param R The return type of both transformation functions
 * @param onSuccess Function to transform the success data
 * @param onFailure Function to transform the error
 * @return The result of applying the appropriate transformation function
 */
inline fun <R, T, E> AppResult<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R
): R {
    return when (this) {
        is AppResult.Success -> onSuccess(data)
        is AppResult.Failure -> onFailure(error)
    }
}