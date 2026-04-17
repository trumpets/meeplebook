package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerListReducerTest {

    private val reducer = PlayerListReducer()

    @Test
    fun `AddNewPlayer increases player list size by one`() {
        val result = reducer.reduce(emptyList(), AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertEquals(1, result.size)
    }

    @Test
    fun `AddNewPlayer new entry has null score and isWinner false`() {
        val result = reducer.reduce(emptyList(), AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        val player = result.first()
        assertNull(player.score)
        assertFalse(player.isWinner)
    }

    @Test
    fun `AddNewPlayer new entry has the given startPosition`() {
        val result = reducer.reduce(emptyList(), AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 3))
        assertEquals(3, result.first().startPosition)
    }

    @Test
    fun `AddNewPlayer new entry has set name`() {
        val result = reducer.reduce(emptyList(), AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 1))
        assertEquals("Ivo", result.first().playerIdentity.name)
    }

    @Test
    fun `AddNewPlayer appends to existing list`() {
        val existing = listOf(makePlayer(makeIdentity("Alice")))
        val result = reducer.reduce(existing, AddPlayEvent.PlayerListEvent.AddNewPlayer(playerName = "Ivo", startPosition = 2))
        assertEquals(2, result.size)
        assertEquals("Alice", result[0].playerIdentity.name)
        assertEquals("Ivo", result[1].playerIdentity.name)
    }

    @Test
    fun `AddPlayerFromSuggestion adds player with correct identity`() {
        val identity = makeIdentity("Bob", username = "bobgames", userId = 42L)
        val result = reducer.reduce(
            emptyList(),
            AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(playerIdentity = identity, startPosition = 1)
        )
        assertEquals(1, result.size)
        assertEquals(identity, result.first().playerIdentity)
    }

    @Test
    fun `AddPlayerFromSuggestion new entry has score zero and isWinner false`() {
        val identity = makeIdentity("Bob")
        val result = reducer.reduce(
            emptyList(),
            AddPlayEvent.PlayerListEvent.AddPlayerFromSuggestion(playerIdentity = identity, startPosition = 1)
        )
        assertEquals(0.0, result.first().score)
        assertFalse(result.first().isWinner)
    }

    @Test
    fun `RemovePlayer removes the matching player`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice), makePlayer(bob))
        val result = reducer.reduce(players, AddPlayEvent.PlayerListEvent.RemovePlayer(playerIdentity = alice))
        assertEquals(1, result.size)
        assertEquals(bob, result.first().playerIdentity)
    }

    @Test
    fun `RemovePlayer with unknown identity leaves list unchanged`() {
        val alice = makeIdentity("Alice")
        val unknown = makeIdentity("Nobody")
        val players = listOf(makePlayer(alice))
        val result = reducer.reduce(players, AddPlayEvent.PlayerListEvent.RemovePlayer(playerIdentity = unknown))
        assertEquals(1, result.size)
    }

    @Test
    fun `EditPlayer and StopEditingPlayer pass through unchanged`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice))
        val afterEdit = reducer.reduce(players, AddPlayEvent.PlayerListEvent.EditPlayer(playerIdentity = alice))
        val afterStop = reducer.reduce(players, AddPlayEvent.PlayerListEvent.StopEditingPlayer)
        assertEquals(players, afterEdit)
        assertEquals(players, afterStop)
    }
}
