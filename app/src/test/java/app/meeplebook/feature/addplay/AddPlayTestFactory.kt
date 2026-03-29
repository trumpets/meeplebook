package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import java.time.Instant

/**
 * Factory helpers for constructing AddPlay test state without boilerplate.
 */
object AddPlayTestFactory {

    fun makeIdentity(
        name: String = "Player",
        username: String? = null,
        userId: Long? = null
    ): PlayerIdentity = PlayerIdentity(name = name, username = username, userId = userId)

    fun makePlayer(
        identity: PlayerIdentity = makeIdentity(),
        startPosition: Int = 1,
        score: Int? = null,
        isWinner: Boolean = false,
        color: String? = null
    ): PlayerEntryUi = PlayerEntryUi(
        playerIdentity = identity,
        startPosition = startPosition,
        color = color,
        score = score,
        isWinner = isWinner
    )

    fun makeState(
        players: List<PlayerEntryUi> = emptyList(),
        date: Instant = Instant.parse("2024-06-01T10:00:00Z"),
        durationMinutes: Int? = null,
        locationValue: String = "",
        gameId: Long? = 1L,
        gameName: String? = "Test Game"
    ): AddPlayUiState = AddPlayUiState(
        gameId = gameId,
        gameName = gameName,
        date = date,
        durationMinutes = durationMinutes,
        location = LocationState(
            value = locationValue,
            suggestions = emptyList(),
            recentLocations = emptyList(),
            isFocused = false
        ),
        players = PlayersState(
            players = players,
            colorsHistory = PlayerColor.entries.map { it.colorString }
        ),
        playersByLocation = emptyList(),
        isSaving = false
    )
}
