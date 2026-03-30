package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSearchState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSelectedState
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.requireGameSearch
import app.meeplebook.feature.addplay.requireGameSelected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSearchReducerTest {

    private val reducer = GameSearchReducer()

    @Test
    fun `GameSearchQueryChanged updates gameSearchQuery`() {
        val result = reducer.reduce(
            makeGameSearchState(),
            AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Catan")
        )
        assertEquals("Catan", result.requireGameSearch().gameSearchQuery)
    }

    @Test
    fun `GameSearchQueryChanged leaves other state unchanged`() {
        val state = makeGameSearchState(gameId = null, gameName = null)
        val result = reducer.reduce(state, AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Wing"))
        val search = result.requireGameSearch()
        assertNull(search.gameId)
        assertNull(search.gameName)
    }

    @Test
    fun `GameSelected sets gameId and gameName`() {
        val result = reducer.reduce(
            makeGameSearchState(),
            AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan")
        )
        val selected = result.requireGameSelected()
        assertEquals(42L, selected.gameId)
        assertEquals("Wingspan", selected.gameName)
    }

    @Test
    fun `GameSelected transitions from GameSearch to GameSelected state`() {
        val state = makeGameSearchState(gameSearchQuery = "Wing")
        val result = reducer.reduce(state, AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan"))
        assertTrue(result is AddPlayUiState.GameSelected)
        val selected = result.requireGameSelected()
        assertEquals(42L, selected.gameId)
        assertEquals("Wingspan", selected.gameName)
    }

    @Test
    fun `non-GameSearch event on GameSearch state leaves state unchanged`() {
        val state = makeGameSearchState()
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.LocationChanged("Home"))
        assertEquals(state, result)
    }

    @Test
    fun `any event on GameSelected state is passed through unchanged`() {
        val state = makeGameSelectedState()
        val result = reducer.reduce(state, AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Catan"))
        assertEquals(state, result)
    }
}
