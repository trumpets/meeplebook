package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.Play
import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.plays.model.PlaySyncStatus
import app.meeplebook.core.plays.model.Player
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import java.time.Instant

/**
 * Immutable state consumed by the Add Play screen.
 */
data class AddPlayUiState(
    val gameId: Long,
    val gameName: String,

    val date: Instant = Instant.now(),
    val durationMinutes: Int?,
    val location: LocationState,

    val players: PlayersState,

    val playersByLocation: List<PlayerSuggestion>,

    val quantity: Int = 1,
    val incomplete: Boolean = false,
    val comments: String = "",

    val isSaving: Boolean,
    val error: UiText = uiTextEmpty()
) {

    /**
     * True when the current state has enough data to create a valid play record.
     *
     * The game name must be non-blank (it is always provided when the screen is opened),
     * and a save must not already be in progress.
     */
    val canSave: Boolean
        get() = gameName.isNotBlank() && !isSaving

    /**
     * Maps the current UI state to a domain [Play] ready to be persisted.
     *
     * The resulting play uses [PlayId.Local] with id `0` because it has not yet been
     * written to the database.  Its [PlaySyncStatus] is [PlaySyncStatus.PENDING] because
     * it will need to be synced with BGG after the initial save.
     */
    fun toDomain(): Play = Play(
        playId = PlayId.Local(localId = 0),
        date = date,
        quantity = quantity,
        length = durationMinutes,
        incomplete = incomplete,
        location = location.value.takeIf { it.isNotBlank() },
        gameId = gameId,
        gameName = gameName,
        comments = comments.takeIf { it.isNotBlank() },
        players = players.players.map { entry ->
            Player(
                id = 0,
                playId = 0,
                username = entry.playerIdentity.username,
                userId = entry.playerIdentity.userId,
                name = entry.playerIdentity.name,
                startPosition = entry.startPosition,
                color = entry.color,
                score = entry.score,
                win = entry.isWinner
            )
        },
        syncStatus = PlaySyncStatus.PENDING
    )
}

/**
 * Location input state, including autocomplete suggestions and focus state.
 */
data class LocationState(
    val value: String,
    val suggestions: List<String>,
    val recentLocations: List<String>,
    val isFocused: Boolean
)

/**
 * Player list state for the Add Play form.
 */
data class PlayersState(
    val players: List<PlayerEntryUi>,
    val colorsHistory: List<String>,
//    val editingPlayerId: PlayerEntryId?
)

/**
 * UI model for a player entry in the Add Play form.
 */
data class PlayerEntryUi(
    val playerIdentity: PlayerIdentity,

    val startPosition: Int?,
    val color: String?,

    val score: Int?,
    val isWinner: Boolean,
) {
    companion object {
        /**
         * Creates an empty player row ready for manual input.
         */
        fun empty(playerName: String, startPosition: Int): PlayerEntryUi {
            return PlayerEntryUi(
                playerIdentity = PlayerIdentity(name = playerName, username = null, userId = null),
                startPosition = startPosition,
                color = null,
                score = null,
                isWinner = false
            )
        }

        /**
         * Creates a pre-filled player row from an existing identity suggestion.
         */
        fun fromPlayerIdentity(playerIdentity: PlayerIdentity, startPosition: Int): PlayerEntryUi {
            return PlayerEntryUi(
                playerIdentity = playerIdentity,
                startPosition = startPosition,
                color = null,
                score = 0,
                isWinner = false
            )
        }
    }
}

/**
 * Suggested player for quick-add based on location history.
 */
data class PlayerSuggestion(
    val playerIdentity: PlayerIdentity,
    val playCount: Int
)