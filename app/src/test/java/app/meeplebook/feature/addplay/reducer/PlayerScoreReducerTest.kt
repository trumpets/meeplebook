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
        val players = listOf(makePlayer(alice, score = 0))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 15))
        assertEquals(15, result.first().score)
    }

    @Test
    fun `ScoreChanged marks highest-scoring player as winner`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 5), makePlayer(bob, score = 3))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 10))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged clears winner flag from previous winner when outscored`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 10, isWinner = true), makePlayer(bob, score = 3))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 20))
        assertFalse(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged with tied scores marks both players as winners`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 10), makePlayer(bob, score = 5))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(bob, 10))
        assertTrue(result.first { it.playerIdentity == alice }.isWinner)
        assertTrue(result.first { it.playerIdentity == bob }.isWinner)
    }

    @Test
    fun `ScoreChanged on single player marks that player as winner`() {
        val alice = makeIdentity("Alice")
        val players = listOf(makePlayer(alice, score = null))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 5))
        assertTrue(result.first().isWinner)
    }

    @Test
    fun `ScoreChanged leaves other players scores intact`() {
        val alice = makeIdentity("Alice")
        val bob = makeIdentity("Bob")
        val players = listOf(makePlayer(alice, score = 7), makePlayer(bob, score = 3))
        val result = reducer.reduce(players, AddPlayEvent.PlayerScoreEvent.ScoreChanged(alice, 12))
        assertEquals(3, result.first { it.playerIdentity == bob }.score)
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
}
