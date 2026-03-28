package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.ui.UiText
import app.meeplebook.core.ui.uiTextEmpty
import java.time.Instant

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
)

data class LocationState(
    val value: String,
    val suggestions: List<String>,
    val isFocused: Boolean
)

data class PlayersState(
    val players: List<PlayerEntryUi>,
    val availableColors: List<String>,
//    val editingPlayerId: PlayerEntryId?
)

data class PlayerEntryUi(
    val playerIdentity: PlayerIdentity,

    val startPosition: Int?,
    val color: String?,

    val score: Int?,
    val isWinner: Boolean,
) {
    companion object {
        fun empty(startPosition: Int): PlayerEntryUi {
            return PlayerEntryUi(
                playerIdentity = PlayerIdentity(name = "", username = null, userId = null),
                startPosition = startPosition,
                color = null,
                score = null,
                isWinner = false
            )
        }

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

data class PlayerSuggestion(
    val playerIdentity: PlayerIdentity,
    val playCount: Int
)