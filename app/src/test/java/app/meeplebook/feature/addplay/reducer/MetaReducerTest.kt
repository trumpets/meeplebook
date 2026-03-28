package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddEmptyPlayer(startPosition = 1))
        assertEquals(state, result)
    }
}
