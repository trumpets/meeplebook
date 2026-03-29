package app.meeplebook.feature.addplay

import app.meeplebook.core.collection.domain.DomainCollectionItem
import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.CreatePlayerCommand
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import java.time.Instant

/**
 * Immutable state consumed by the Add Play screen.
 */
data class AddPlayUiState(
    val gameId: Long?,
    val gameName: String?,

    // Path 1: game search (shown when gameId is null)
    val gameSearchQuery: String = "",
    val gameSearchResults: List<DomainCollectionItem> = emptyList(),

    val date: Instant = Instant.now(),
    val durationMinutes: Int?,
    val location: LocationState,

    val players: PlayersState,

    val playersByLocation: List<PlayerSuggestion>,

    val quantity: Int = 1,
    val incomplete: Boolean = false,
    val comments: String = "",

    val isSaving: Boolean,
    val error: UiText = uiTextEmpty(),

    val canSave: Boolean = false
) {
    companion object {
        /** Creates the default initial state; gameId/gameName arrive via [AddPlayEvent.GameSearchEvent.GameSelected]. */
        fun initial() = AddPlayUiState(
            gameId = null,
            gameName = null,
            durationMinutes = null,
            location = LocationState(
                value = null,
                suggestions = emptyList(),
                recentLocations = emptyList(),
                isFocused = false
            ),
            players = PlayersState(players = emptyList(), colorsHistory = emptyList()),
            playersByLocation = emptyList(),
            isSaving = false
        )
    }

    fun toCreatePlayCommand(): CreatePlayCommand {
        return CreatePlayCommand(
            gameId = requireNotNull(gameId) { "Cannot create play command: gameId is null" },
            gameName = requireNotNull(gameName) { "Cannot create play command: gameName is null" },
            date = date,
            length = durationMinutes,
            location = location.value,
            players = players.players
                .filter { it.playerIdentity.name.isNotBlank() }
                .map {
                    CreatePlayerCommand(
                        name = it.playerIdentity.name,
                        username = it.playerIdentity.username,
                        userId = it.playerIdentity.userId,
                        startPosition = it.startPosition,
                        color = it.color,
                        score = it.score,
                        win = it.isWinner
                    )
                },
            quantity = quantity,
            incomplete = incomplete,
            comments = comments.ifBlank { null }
        )
    }
}

/**
 * Location input state, including autocomplete suggestions and focus state.
 */
data class LocationState(
    val value: String?,
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

    val startPosition: Int,
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