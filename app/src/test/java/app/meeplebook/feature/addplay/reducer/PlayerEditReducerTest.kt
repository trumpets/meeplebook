package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerEditReducerTest {

    private val reducer = PlayerEditReducer()

    @Test
    fun `NameChanged updates name on the target player`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.NameChanged(alice, "Alicia"))
        assertEquals("Alicia", result.first().playerIdentity.name)
    }

    @Test
    fun `NameChanged does not affect other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice), makePlayer(bob))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.NameChanged(alice, "Alicia"))
        assertEquals("Bob", result.first { it.playerIdentity == bob }.playerIdentity.name)
    }

    @Test
    fun `UsernameChanged updates username on the target player`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.UsernameChanged(alice, "alice_bgg"))
        assertEquals("alice_bgg", result.first().playerIdentity.username)
    }

    @Test
    fun `UsernameChanged does not affect other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob", username = "bob_bgg")
        val players = listOf(makePlayer(alice), makePlayer(bob))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.UsernameChanged(alice, "alice_bgg"))
        assertEquals("bob_bgg", result.first { it.playerIdentity == bob }.playerIdentity.username)
    }

    @Test
    fun `TeamChanged updates color on the target player`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, color = null))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.TeamChanged(alice, "Blue"))
        assertEquals("Blue", result.first().color)
    }

    @Test
    fun `TeamChanged does not affect other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, color = null), makePlayer(bob, color = "Red"))
        val result = reducer.reduce(players, AddPlayEvent.PlayerEditEvent.TeamChanged(alice, "Blue"))
        assertEquals("Red", result.first { it.playerIdentity == bob }.color)
    }
}
