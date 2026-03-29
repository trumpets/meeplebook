package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayTestFactory.makeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationReducerTest {

    private val reducer = ValidationReducer()

    @Test
    fun `canSave is true when both gameId and gameName are non-null`() {
        val state = makeState(gameId = 1L, gameName = "Wingspan")
        val result = reducer.reduce(state)
        assertTrue(result.canSave)
    }

    @Test
    fun `canSave is false when gameId is null`() {
        val state = makeState(gameId = null, gameName = "Wingspan")
        val result = reducer.reduce(state)
        assertFalse(result.canSave)
    }

    @Test
    fun `canSave is false when gameName is null`() {
        val state = makeState(gameId = 1L, gameName = null)
        val result = reducer.reduce(state)
        assertFalse(result.canSave)
    }

    @Test
    fun `canSave is false when both gameId and gameName are null`() {
        val state = makeState(gameId = null, gameName = null)
        val result = reducer.reduce(state)
        assertFalse(result.canSave)
    }

    @Test
    fun `canSave is overridden to false when gameId is null even if previous canSave was true`() {
        val state = makeState(gameId = null, gameName = "Wingspan").copy(canSave = true)
        val result = reducer.reduce(state)
        assertFalse(result.canSave)
    }
}
