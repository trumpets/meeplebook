package app.meeplebook.feature.addplay.reducer

import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerColorReducerTest {

    private val reducer = PlayerColorReducer()

    @Test
    fun `ColorSelected sets the color string on the target player`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, color = null))
        val result = reducer.reduce(
            players,
            AddPlayEvent.PlayerColorEvent.ColorSelected(alice, PlayerColor.RED)
        )
        assertEquals(PlayerColor.RED.colorString, result.first().color)
    }

    @Test
    fun `ColorSelected does not affect other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, color = null), makePlayer(bob, color = "Blue"))
        val result = reducer.reduce(
            players,
            AddPlayEvent.PlayerColorEvent.ColorSelected(alice, PlayerColor.GREEN)
        )
        assertEquals("Blue", result.first { it.playerIdentity == bob }.color)
    }

    @Test
    fun `ColorClicked passes through list unchanged`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, color = "Red"))
        val result = reducer.reduce(
            players,
            AddPlayEvent.PlayerColorEvent.ColorClicked(alice)
        )
        assertEquals(players, result)
    }
}
