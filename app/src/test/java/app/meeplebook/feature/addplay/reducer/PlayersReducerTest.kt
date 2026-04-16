package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSelectedState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import app.meeplebook.feature.addplay.requireGameSelected
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class PlayersReducerTest {

    private val reducer = PlayersReducer(
        editReducer = PlayerEditReducer(),
        listReducer = PlayerListReducer(),
        scoreReducer = PlayerScoreReducer(),
        colorReducer = PlayerColorReducer(),
    )

    @Test
    fun `PlayerListEvent is routed to PlayerListReducer`() {
        val state = makeGameSelectedState(players = emptyList())
        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertEquals(1, result.requireGameSelected().players.players.size)
    }

    @Test
    fun `PlayerScoreEvent is routed to PlayerScoreReducer`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice, score = 0.0)))
        val result = reducer.reduce(state, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 20.0))
        assertEquals(20.0, result.requireGameSelected().players.players.first().score)
    }

    @Test
    fun `PlayerEditEvent is routed to PlayerEditReducer`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice)))
        val result = reducer.reduce(state, AddPlayEvent.PlayerEditEvent.NameChanged(alice, "Alicia"))
        assertEquals("Alicia", result.requireGameSelected().players.players.first().playerIdentity.name)
    }

    @Test
    fun `PlayerColorEvent is routed to PlayerColorReducer`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice, color = null)))
        val result = reducer.reduce(
            state,
            AddPlayEvent.PlayerColorEvent.ColorSelected(alice, PlayerColor.BLUE)
        )
        assertEquals(PlayerColor.BLUE.colorString, result.requireGameSelected().players.players.first().color)
    }

    @Test
    fun `MetadataEvent leaves player list unchanged`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice)))
        val result = reducer.reduce(
            state,
            AddPlayEvent.MetadataEvent.DateChanged(Instant.parse("2025-01-01T00:00:00Z"))
        )
        assertEquals(state.players.players, result.requireGameSelected().players.players)
    }
}
