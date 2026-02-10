package app.meeplebook.core.plays

import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
import java.time.Instant

/**
 * Factory for creating test Play objects.
 * Provides reusable test data creation methods to avoid duplication across test files.
 */
object PlayTestFactory {

    /**
     * Creates a test Play object with default or custom values.
     *
     * @param localPlayId The local play ID
     * @param gameName The name of the game
     * @param date The play date (defaults to 2024-01-15T20:00:00Z)
     * @param quantity Number of plays (defaults to 1)
     * @param length Play duration in minutes (defaults to 60)
     * @param incomplete Whether the play was incomplete (defaults to false)
     * @param location Play location (defaults to null)
     * @param gameId The game ID (defaults to id * 100)
     * @param comments Play comments (defaults to null)
     * @param players List of players (defaults to a single test player)
     * @return A configured Play object
     */
    fun createPlay(
        localPlayId: Long,
        gameName: String,
        date: Instant = Instant.parse("2024-01-15T20:00:00Z"),
        quantity: Int = 1,
        length: Int? = 60,
        incomplete: Boolean = false,
        location: String? = null,
        gameId: Long = localPlayId * 100,
        comments: String? = null,
        players: List<Player> = listOf(createPlayer(playId = localPlayId)),
        syncStatus: PlaySyncStatus = PlaySyncStatus.SYNCED
    ): Play {
        return Play(
            playId = PlayId.Local(localPlayId),
            date = date,
            quantity = quantity,
            length = length,
            incomplete = incomplete,
            location = location,
            gameId = gameId,
            gameName = gameName,
            comments = comments,
            players = players,
            syncStatus = syncStatus
        )
    }

    /**
     * Creates a test Player object with default or custom values.
     *
     * @param id The player record ID (defaults to 0)
     * @param playId The play ID this player is associated with
     * @param username The player's username (defaults to "testuser")
     * @param userId The player's user ID (defaults to 12345)
     * @param name The player's display name (defaults to "Test User")
     * @param startPosition The player's starting position (defaults to null)
     * @param color The player's color (defaults to null)
     * @param score The player's score (defaults to null)
     * @param win Whether the player won (defaults to false)
     * @return A configured Player object
     */
    fun createPlayer(
        id: Long = 0,
        playId: Long,
        username: String? = "testuser",
        userId: Long? = 12345,
        name: String = "Test User",
        startPosition: String? = null,
        color: String? = null,
        score: Int? = null,
        win: Boolean = false
    ): Player {
        return Player(
            id = id,
            playId = playId,
            username = username,
            userId = userId,
            name = name,
            startPosition = startPosition,
            color = color,
            score = score,
            win = win
        )
    }
}
