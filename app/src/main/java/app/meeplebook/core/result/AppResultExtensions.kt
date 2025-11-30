package app.meeplebook.core.result

inline fun <R, T, E> AppResult<T, E>.fold(
    onSuccess: (T) -> R,
    onFailure: (E) -> R
): R {
    return when (this) {
        is AppResult.Success -> onSuccess(data)
        is AppResult.Failure   -> onFailure(error)
    }
}