package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.CreatePlayCommand
import app.meeplebook.core.plays.domain.CreatePlayerCommand
import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import java.time.Instant

/**
 * Immutable state consumed by the Add Play screen.
 */
sealed interface AddPlayUiState {

    data class GameSearch(
        val gameId: Long? = null,
        val gameName: String? = null,

        // Path 1: game search (shown when gameId is null)
        val gameSearchQuery: String = "",
        val gameSearchResults: List<SearchResultGameItem> = emptyList(),
    ) : AddPlayUiState

    data class GameSelected(
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

        val showQuantity: Boolean = false,
        val showIncomplete: Boolean = false,
        val showComments: Boolean = false,

        val isSaving: Boolean,
        val error: UiText = uiTextEmpty(),
    ) : AddPlayUiState {

        val canSave: Boolean
            get() = gameName.isNotBlank() && !isSaving

        fun toCreatePlayCommand(): CreatePlayCommand {
            return CreatePlayCommand(
                gameId = gameId,
                gameName = gameName,
                date = date,
                length = durationMinutes,
                location = location.value?.takeIf { it.isNotBlank() },
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
}

/**
 * Location input state, including autocomplete suggestions and focus state.
 */
data class LocationState(
    val value: String?,
    val suggestions: List<String>,
    val recentLocations: List<String>
)

/**
 * Player list state for the Add Play form.
 */
data class PlayersState(
    val players: List<PlayerEntryUi>,
    val colorsHistory: List<PlayerColor>
)

/**
 * UI model for a player entry in the Add Play form.
 */
data class PlayerEntryUi(
    val playerIdentity: PlayerIdentity,

    val startPosition: Int,
    val color: String?,

    val score: Double?,
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
                score = 0.0,
                isWinner = false
            )
        }
    }
}

/**
 * Suggested player for quick-add based on location history.
 */
data class PlayerSuggestion(
    val playerIdentity: PlayerIdentity
)

data class SearchResultGameItem(
    val gameId: Long,
    val name: String,
    val yearPublished: Int?,
    val thumbnailUrl: String?,
)

/**
 * Safely updates the current state if it is GameSelected.
 *
 * Usage:
 * _uiState.value = _uiState.value.updateGameSelected { copy(isSaving = true) }
 */
fun AddPlayUiState.updateGameSelected(
    block: AddPlayUiState.GameSelected.() -> AddPlayUiState.GameSelected
): AddPlayUiState {
    return when (this) {
        is AddPlayUiState.GameSelected -> block()
        is AddPlayUiState.GameSearch -> this // leave unchanged
    }
}

/**
 * Safely transforms the current state if it is GameSearch.
 * The block can return any [AddPlayUiState], enabling both in-place
 * updates and state transitions (e.g. GameSearch → GameSelected).
 *
 * Usage:
 * _uiState.value = _uiState.value.updateGameSearch { copy(gameSearchQuery = "Cata") }
 */
fun AddPlayUiState.updateGameSearch(
    block: AddPlayUiState.GameSearch.() -> AddPlayUiState
): AddPlayUiState {
    return when (this) {
        is AddPlayUiState.GameSelected -> this // leave unchanged
        is AddPlayUiState.GameSearch -> block()
    }
}

/**
 * Safely run [block] if the state is [AddPlayUiState.GameSelected], otherwise return null.
 */
inline fun <T> AddPlayUiState.asGameSelected(block: AddPlayUiState.GameSelected.() -> T): T? {
    return (this as? AddPlayUiState.GameSelected)?.block()
}