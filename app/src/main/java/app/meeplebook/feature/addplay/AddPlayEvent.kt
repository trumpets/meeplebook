package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import java.time.Instant

sealed interface AddPlayEvent {

    sealed interface MetadataEvent : AddPlayEvent {

        data class DateChanged(val date: Instant) : MetadataEvent

        data class DurationChanged(val minutes: Int?) : MetadataEvent

        data class LocationChanged(val value: String) : MetadataEvent

        data class LocationSuggestionSelected(val value: String) : MetadataEvent
    }

    sealed interface PlayerListEvent : AddPlayEvent {

        data class AddEmptyPlayer(
            val startPosition: Int
        ) : PlayerListEvent

        data class AddPlayerFromSuggestion(
            val playerId: PlayerIdentity,
            val startPosition: Int
        ) : PlayerListEvent

        data class RemovePlayer(
            val playerEntryId: PlayerIdentity
        ) : PlayerListEvent

        data class EditPlayer(
            val playerEntryId: PlayerIdentity
        ) : PlayerListEvent

        data object StopEditingPlayer : PlayerListEvent
    }

    sealed interface PlayerEditEvent : AddPlayEvent {

        data class NameChanged(
            val playerEntryId: PlayerIdentity,
            val name: String
        ) : PlayerEditEvent

        data class UsernameChanged(
            val playerEntryId: PlayerIdentity,
            val username: String
        ) : PlayerEditEvent

        data class TeamChanged(
            val playerEntryId: PlayerIdentity,
            val team: String
        ) : PlayerEditEvent
    }

    sealed interface PlayerScoreEvent : AddPlayEvent {

        data class ScoreChanged(
            val playerEntryId: PlayerIdentity,
            val score: Int
        ) : PlayerScoreEvent

        data class WinnerToggled(
            val playerEntryId: PlayerIdentity,
            val isWinner: Boolean
        ) : PlayerScoreEvent
    }

    sealed interface PlayerColorEvent : AddPlayEvent {

        data class ColorClicked(
            val playerEntryId: PlayerIdentity
        ) : PlayerColorEvent

        data class ColorSelected(
            val playerEntryId: PlayerIdentity,
            val color: PlayerColor
        ) : PlayerColorEvent
    }

    sealed interface SuggestionEvent : AddPlayEvent {

        data object RefreshPlayerSuggestions : SuggestionEvent
    }

    sealed interface ActionEvent : AddPlayEvent {

        data object SaveClicked : ActionEvent

        data object CancelClicked : ActionEvent
    }
}

