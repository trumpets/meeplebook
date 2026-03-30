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
import java.time.Instant

class MetaReducerTest {

    private val reducer = MetaReducer()

    @Test
    fun `DateChanged updates the play date`() {
        val newDate = Instant.parse("2025-01-01T12:00:00Z")
        val result = reducer.reduce(makeGameSelectedState(), AddPlayEvent.MetadataEvent.DateChanged(newDate))
        assertEquals(newDate, result.requireGameSelected().date)
    }

    @Test
    fun `DurationChanged updates duration minutes`() {
        val result = reducer.reduce(makeGameSelectedState(), AddPlayEvent.MetadataEvent.DurationChanged(90))
        assertEquals(90, result.requireGameSelected().durationMinutes)
    }

    @Test
    fun `DurationChanged with null clears duration`() {
        val state = makeGameSelectedState(durationMinutes = 60)
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DurationChanged(null))
        assertNull(result.requireGameSelected().durationMinutes)
    }

    @Test
    fun `LocationChanged updates location value`() {
        val result = reducer.reduce(makeGameSelectedState(), AddPlayEvent.MetadataEvent.LocationChanged("Home"))
        assertEquals("Home", result.requireGameSelected().location.value)
    }

    @Test
    fun `LocationSuggestionSelected updates location value`() {
        val result = reducer.reduce(
            makeGameSelectedState(),
            AddPlayEvent.MetadataEvent.LocationSuggestionSelected("Game Cafe")
        )
        assertEquals("Game Cafe", result.requireGameSelected().location.value)
    }

    @Test
    fun `non-metadata event leaves state unchanged`() {
        val state = makeGameSelectedState(locationValue = "Somewhere")
        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertEquals(state, result)
    }

    // ── GameSearchEvent ──────────────────────────────────────────────────────

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
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Wing")
        )
        val search = result.requireGameSearch()
        assertNull(search.gameId)
        assertNull(search.gameName)
    }

    @Test
    fun `GameSelected sets gameId and gameName`() {
        val state = makeGameSearchState()
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan")
        )
        val selected = result.requireGameSelected()
        assertEquals(42L, selected.gameId)
        assertEquals("Wingspan", selected.gameName)
    }

    @Test
    fun `GameSelected transitions from GameSearch to GameSelected state`() {
        val state = makeGameSearchState(gameSearchQuery = "Wing")
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan")
        )
        // Transitioning to GameSelected means game search state no longer exists
        assertTrue(result is AddPlayUiState.GameSelected)
        val selected = result.requireGameSelected()
        assertEquals(42L, selected.gameId)
        assertEquals("Wingspan", selected.gameName)
    }
}
