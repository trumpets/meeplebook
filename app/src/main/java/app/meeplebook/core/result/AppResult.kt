package app.meeplebook.core.result

sealed interface AppResult<out T, out E> {
    data class Success<T>(val data: T) : AppResult<T, Nothing>
    data class Failure<E>(val error: E) : AppResult<Nothing, E>
}