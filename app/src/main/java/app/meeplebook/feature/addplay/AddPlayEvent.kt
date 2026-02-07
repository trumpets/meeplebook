package app.meeplebook.feature.addplay

import java.time.Instant

/**
 * Represents user actions and system events for the Add Play screen.
 */
sealed interface AddPlayEvent {

    // Game selection
    data class GameSearchQueryChanged(val query: String) : AddPlayEvent
    data class GameSelected(val gameId: Long, val gameName: String) : AddPlayEvent

    // Basic play info
    data class DateChanged(val date: Instant) : AddPlayEvent
    data class DurationChanged(val duration: String) : AddPlayEvent
    data class LocationChanged(val location: String) : AddPlayEvent
    data class CommentsChanged(val comments: String) : AddPlayEvent

    // Player management
    data class AddPlayerClicked(val suggestedPlayer: String? = null) : AddPlayEvent
    data class RemovePlayerClicked(val playerId: String) : AddPlayEvent
    data class PlayerExpandToggled(val playerId: String) : AddPlayEvent

    // Player details
    data class PlayerNameChanged(val playerId: String, val name: String) : AddPlayEvent
    data class PlayerPositionChanged(val playerId: String, val position: String) : AddPlayEvent
    data class PlayerColorClicked(val playerId: String) : AddPlayEvent
    data class PlayerColorSelected(val playerId: String, val color: String) : AddPlayEvent
    data class PlayerScoreChanged(val playerId: String, val score: String) : AddPlayEvent
    data class PlayerWinToggled(val playerId: String) : AddPlayEvent
    data class PlayerTeamChanged(val playerId: String, val team: String) : AddPlayEvent

    // Quick score input (from player list)
    data class PlayerQuickScoreChanged(val playerId: String, val score: String) : AddPlayEvent

    // New player addition
    data class NewPlayerNameChanged(val name: String) : AddPlayEvent
    data object ShowAddPlayerDialog : AddPlayEvent
    data object HideAddPlayerDialog : AddPlayEvent
    data object ConfirmAddPlayer : AddPlayEvent

    // Actions
    data object SavePlayClicked : AddPlayEvent
    data object CancelClicked : AddPlayEvent
    data object DismissError : AddPlayEvent

    // Color picker
    data object DismissColorPicker : AddPlayEvent
}
