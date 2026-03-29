package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeState
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
        val result = reducer.reduce(makeState(), AddPlayEvent.MetadataEvent.DateChanged(newDate))
        assertEquals(newDate, result.date)
    }

    @Test
    fun `DurationChanged updates duration minutes`() {
        val result = reducer.reduce(makeState(), AddPlayEvent.MetadataEvent.DurationChanged(90))
        assertEquals(90, result.durationMinutes)
    }

    @Test
    fun `DurationChanged with null clears duration`() {
        val state = makeState(durationMinutes = 60)
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DurationChanged(null))
        assertNull(result.durationMinutes)
    }

    @Test
    fun `LocationChanged updates location value`() {
        val result = reducer.reduce(makeState(), AddPlayEvent.MetadataEvent.LocationChanged("Home"))
        assertEquals("Home", result.location.value)
    }

    @Test
    fun `LocationSuggestionSelected updates location value`() {
        val result = reducer.reduce(
            makeState(),
            AddPlayEvent.MetadataEvent.LocationSuggestionSelected("Game Cafe")
        )
        assertEquals("Game Cafe", result.location.value)
    }

    @Test
    fun `non-metadata event leaves state unchanged`() {
        val state = makeState(locationValue = "Somewhere")
        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertEquals(state, result)
    }

    // ── GameSearchEvent ──────────────────────────────────────────────────────

    @Test
    fun `GameSearchQueryChanged updates gameSearchQuery`() {
        val result = reducer.reduce(
            makeState(),
            AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Catan")
        )
        assertEquals("Catan", result.gameSearchQuery)
    }

    @Test
    fun `GameSearchQueryChanged leaves other state unchanged`() {
        val state = makeState(gameId = null, gameName = null)
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSearchQueryChanged("Wing")
        )
        assertNull(result.gameId)
        assertNull(result.gameName)
    }

    @Test
    fun `GameSelected sets gameId and gameName`() {
        val state = makeState(gameId = null, gameName = null)
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan")
        )
        assertEquals(42L, result.gameId)
        assertEquals("Wingspan", result.gameName)
    }

    @Test
    fun `GameSelected clears game search fields`() {
        val state = makeState(gameId = null, gameName = null)
            .copy(gameSearchQuery = "Wing", gameSearchResults = emptyList())
        val result = reducer.reduce(
            state,
            AddPlayEvent.GameSearchEvent.GameSelected(gameId = 42L, gameName = "Wingspan")
        )
        assertEquals("", result.gameSearchQuery)
        assertTrue(result.gameSearchResults.isEmpty())
    }
}
