package app.meeplebook.feature.addplay.reducer

import app.meeplebook.feature.addplay.AddPlayEvent
import app.meeplebook.feature.addplay.AddPlayTestFactory.makeIdentity
import app.meeplebook.feature.addplay.AddPlayTestFactory.makePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScoreReducerTest {

    private val reducer = PlayerScoreReducer()

    @Test
    fun `ScoreChanged updates the target player score`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, score = 0.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 15.0))
        assertEquals(15.0, result.first().score)
    }

    @Test
    fun `ScoreChanged marks highest-scoring player as winner`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 5.0), makePlayer(bob, score = 3.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 10.0))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged clears winner flag from previous winner when outscored`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 10.0, isWinner = true), makePlayer(bob, score = 3.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 20.0))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged with tied scores marks both players as winners`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 10.0), makePlayer(bob, score = 5.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 10.0))
        assertTrue(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged on single player marks that player as winner`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, score = null))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 5.0))
        assertTrue(result.first().isWinner)
    }

    @Test
    fun `ScoreChanged leaves other players scores intact`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 7.0), makePlayer(bob, score = 3.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 12.0))
        assertEquals(3.0, result.first { it.playerIdentity == bob }.score)
    }

    @Test
    fun `WinnerToggled true sets target player as winner`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, isWinner = false))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.WinnerToggled(alice, true))
        assertTrue(result.first().isWinner)
    }

    @Test
    fun `WinnerToggled false clears target player winner flag`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, isWinner = true))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.WinnerToggled(alice, false))
        assertFalse(result.first().isWinner)
    }

    @Test
    fun `WinnerToggled does not affect other players`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, isWinner = true), makePlayer(bob, isWinner = false))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.WinnerToggled(alice, false))
        assertFalse(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged with null clears score for the target player`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, score = 10.0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, null))
        assertEquals(null, result.first().score)
    }

    @Test
    fun `ScoreChanged with null leaves winner flags unchanged when no remaining scores`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(
            makePlayer(alice, score = 10.0, isWinner = true),
            makePlayer(bob, score = null, isWinner = false),
        )
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, null))
        // No non-null scores remain; winner flags should be left as-is.
        assertTrue(result.first { it.playerIdentity == alice }.isWinner)
        assertFalse(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged with null recalculates winner from remaining non-null scores`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(
            makePlayer(alice, score = 10.0, isWinner = true),
            makePlayer(bob, score = 5.0, isWinner = false),
        )
        // Clearing Alice's score; Bob still has 5.0 and should be auto-marked winner.
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, null))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged with null does not mark null-score player as winner`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(
            makePlayer(alice, score = null, isWinner = false),
            makePlayer(bob, score = 5.0, isWinner = true),
        )
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, null))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }
}
