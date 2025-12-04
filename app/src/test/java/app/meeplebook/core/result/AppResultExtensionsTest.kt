package app.meeplebook.core.result

import org.junit.Assert.assertEquals
import org.junit.Test

class AppResultExtensionsTest {

    @Test
    fun `fold with Success invokes onSuccess`() {
        val result: AppResult<String, String> = AppResult.Success("data")

        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" }
        )

        assertEquals("success: data", folded)
    }

    @Test
    fun `fold with Failure invokes onFailure`() {
        val result: AppResult<String, String> = AppResult.Failure("error")

        val folded = result.fold(
            onSuccess = { "success: $it" },
            onFailure = { "failure: $it" }
        )

        assertEquals("failure: error", folded)
    }

    @Test
    fun `fold transforms Success to different type`() {
        val result: AppResult<Int, String> = AppResult.Success(42)

        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { -1 }
        )

        assertEquals(84, folded)
    }

    @Test
    fun `fold transforms Failure to same return type`() {
        val result: AppResult<Int, String> = AppResult.Failure("error")

        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { -1 }
        )

        assertEquals(-1, folded)
    }

    sealed class Error {
        data object NotFound : Error()
        data class NetworkError(val message: String) : Error()
    }

    @Test
    fun `fold with complex types`() {
        data class User(val name: String)

        val successResult: AppResult<User, Error> = AppResult.Success(User("John"))
        val failureResult: AppResult<User, Error> = AppResult.Failure(Error.NotFound)

        val successMessage = successResult.fold(
            onSuccess = { "Welcome, ${it.name}!" },
            onFailure = { 
                when (it) {
                    is Error.NotFound -> "User not found"
                    is Error.NetworkError -> "Network error: ${it.message}"
                }
            }
        )

        val failureMessage = failureResult.fold(
            onSuccess = { "Welcome, ${it.name}!" },
            onFailure = { 
                when (it) {
                    is Error.NotFound -> "User not found"
                    is Error.NetworkError -> "Network error: ${it.message}"
                }
            }
        )

        assertEquals("Welcome, John!", successMessage)
        assertEquals("User not found", failureMessage)
    }

    @Test
    fun `fold with Unit return type for side effects`() {
        val result: AppResult<String, String> = AppResult.Success("data")
        var sideEffectCalled = false

        result.fold(
            onSuccess = { sideEffectCalled = true },
            onFailure = { }
        )

        assertEquals(true, sideEffectCalled)
    }

    @Test
    fun `fold with nullable return type`() {
        val successResult: AppResult<String, String> = AppResult.Success("data")
        val failureResult: AppResult<String, String> = AppResult.Failure("error")

        val successFolded: String? = successResult.fold(
            onSuccess = { it },
            onFailure = { null }
        )

        val failureFolded: String? = failureResult.fold(
            onSuccess = { it },
            onFailure = { null }
        )

        assertEquals("data", successFolded)
        assertEquals(null, failureFolded)
    }

    @Test
    fun `fold preserves null data from Success`() {
        val result: AppResult<String?, String> = AppResult.Success(null)

        val folded = result.fold(
            onSuccess = { it ?: "was null" },
            onFailure = { "error" }
        )

        assertEquals("was null", folded)
    }
}
