package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSearchState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeGameSelectedState
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import app.meeplebook.feature.addplay.AddPlayUiState
import app.meeplebook.feature.addplay.requireGameSelected
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AddPlayReducerTest {

    private val reducer = AddPlayReducer(
        gameSearchReducer = GameSearchReducer(),
        metaReducer = MetaReducer(),
        playersReducer = PlayersReducer(
            editReducer = PlayerEditReducer(),
            listReducer = PlayerListReducer(),
            scoreReducer = PlayerScoreReducer(),
            colorReducer = PlayerColorReducer(),
        ),
        addEditPlayerDialogReducer = AddEditPlayerDialogReducer(),
    )

    @Test
    fun `MetadataEvent updates play date and leaves players unchanged`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice)))
        val newDate = Instant.parse("2025-12-31T18:00:00Z")

        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DateChanged(newDate)).requireGameSelected()

        assertEquals(newDate, result.date)
        assertEquals(state.players.players, result.players.players)
    }

    @Test
    fun `PlayerListEvent adds player and leaves metadata unchanged`() {
        val state = makeGameSelectedState(players = emptyList())

        val result = reducer.reduce(state, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1)).requireGameSelected()

        assertEquals(1, result.players.players.size)
        assertEquals(state.date, result.date)
    }

    @Test
    fun `canSave is true after event when gameName is set and not saving`() {
        val state = makeGameSelectedState(gameName = "Terraforming Mars")
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DurationChanged(60)).requireGameSelected()
        assertEquals(true, result.canSave)
    }

    @Test
    fun `GameSearch state stays GameSearch after non-transitioning event`() {
        val state = makeGameSearchState()
        val result = reducer.reduce(state, AddPlayEvent.MetadataEvent.DurationChanged(60))
        assertTrue(result is AddPlayUiState.GameSearch)
    }

    @Test
    fun `both reducers run in sequence for independent event types`() {
        val alice = makeIdentity("Alice")
        val state = makeGameSelectedState(players = listOf(makePlayer(alice)))
        val newDate = Instant.parse("2025-06-15T09:00:00Z")

        // Apply date change — meta reducer handles it, players reducer passes through
        val afterDate = reducer.reduce(state, AddPlayEvent.MetadataEvent.DateChanged(newDate)).requireGameSelected()
        assertEquals(newDate, afterDate.date)
        assertEquals(1, afterDate.players.players.size)

        // Apply player add — meta reducer passes through, players reducer handles it
        val afterPlayer = reducer.reduce(afterDate, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 2)).requireGameSelected()
        assertEquals(newDate, afterPlayer.date)
        assertEquals(2, afterPlayer.players.players.size)
    }
}
