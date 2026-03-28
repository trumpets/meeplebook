package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AddPlayReducerTest {

    private val reducer = AddPlayReducer(
        metaReducer = MetaReducer(),
        playersReducer = PlayersReducer(
            editReducer = PlayerEditReducer(),
            listReducer = PlayerListReducer(),
            scoreReducer = PlayerScoreReducer(),
            colorReducer = PlayerColorReducer()
        )
    )

    @Test
    fun `MetadataEvent updates play date and leaves players unchanged`() {
        val alice = makeIdentity("Alice")
        val state = makeState(players = listOf(makePlayer(alice)))
        val newDate = Instant.parse("2025-12-31T18:00:00Z")

        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DateChanged(newDate))

        assertEquals(newDate, result.date)
        assertEquals(state.players.players, result.players.players)
    }

    @Test
    fun `PlayerListEvent adds player and leaves metadata unchanged`() {
        val state = makeState(players = emptyList())

        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddEmptyPlayer(startPosition = 1))

        assertEquals(1, result.players.players.size)
        assertEquals(state.date, result.date)
    }

    @Test
    fun `both reducers run in sequence for independent event types`() {
        val alice = makeIdentity("Alice")
        val state = makeState(players = listOf(makePlayer(alice)))
        val newDate = Instant.parse("2025-06-15T09:00:00Z")

        // Apply date change — meta reducer handles it, players reducer passes through
        val afterDate = reducer.reduce(state, AddPlayEvent.MetadataEvent.DateChanged(newDate))
        assertEquals(newDate, afterDate.date)
        assertEquals(1, afterDate.players.players.size)

        // Apply player add — meta reducer passes through, players reducer handles it
        val afterPlayer = reducer.reduce(afterDate, AddPlayEvent.PlayerListEvent.AddEmptyPlayer(startPosition = 2))
        assertEquals(newDate, afterPlayer.date)
        assertEquals(2, afterPlayer.players.players.size)
    }
}
