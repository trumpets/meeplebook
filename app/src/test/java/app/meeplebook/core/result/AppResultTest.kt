package app.meeplebook.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun `Success wraps data correctly`() {
        val data = "test data"
        val result: AppResult<String, Nothing> = AppResult.Success(data)

        assertTrue(result is AppResult.Success)
        assertEquals(data, (result as AppResult.Success).data)
    }

    @Test
    fun `Failure wraps error correctly`() {
        val error = "test error"
        val result: AppResult<Nothing, String> = AppResult.Failure(error)

        assertTrue(result is AppResult.Failure)
        assertEquals(error, (result as AppResult.Failure).error)
    }

    @Test
    fun `Success with complex type`() {
        data class User(val name: String, val age: Int)
        val user = User("John", 30)
        val result: AppResult<User, String> = AppResult.Success(user)

        assertTrue(result is AppResult.Success)
        assertEquals(user, (result as AppResult.Success).data)
    }

    @Test
    fun `Failure with exception type`() {
        val exception = RuntimeException("Something went wrong")
        val result: AppResult<String, Exception> = AppResult.Failure(exception)

        assertTrue(result is AppResult.Failure)
        assertEquals(exception, (result as AppResult.Failure).error)
    }

    @Test
    fun `Success equals works correctly`() {
        val result1 = AppResult.Success("data")
        val result2 = AppResult.Success("data")
        val result3 = AppResult.Success("other")

        assertEquals(result1, result2)
        assertFalse(result1 == result3)
    }

    @Test
    fun `Failure equals works correctly`() {
        val result1 = AppResult.Failure("error")
        val result2 = AppResult.Failure("error")
        val result3 = AppResult.Failure("other")

        assertEquals(result1, result2)
        assertFalse(result1 == result3)
    }

    @Test
    fun `Success and Failure are not equal`() {
        val success: AppResult<String, String> = AppResult.Success("value")
        val failure: AppResult<String, String> = AppResult.Failure("value")

        assertFalse(success == failure)
    }

    @Test
    fun `Success with null data`() {
        val result: AppResult<String?, Nothing> = AppResult.Success(null)

        assertTrue(result is AppResult.Success)
        assertEquals(null, (result as AppResult.Success).data)
    }

    @Test
    fun `Failure with null error`() {
        val result: AppResult<Nothing, String?> = AppResult.Failure(null)

        assertTrue(result is AppResult.Failure)
        assertEquals(null, (result as AppResult.Failure).error)
    }
}
