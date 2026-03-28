package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import java.time.Instant

/** Top-level union of all user-driven events on the Add Play screen. */
sealed interface AddPlayEvent {

    /** Events that modify the top-level play metadata (date, duration, location). */
    sealed interface MetadataEvent : AddPlayEvent {

        /** User changed the play date. */
        data class DateChanged(val date: Instant) : MetadataEvent

        /** User changed the play duration. [minutes] is `null` when the field is cleared. */
        data class DurationChanged(val minutes: Int?) : MetadataEvent

        /** User typed in the location field. */
        data class LocationChanged(val value: String) : MetadataEvent

        /** User tapped an autocomplete suggestion for the location. */
        data class LocationSuggestionSelected(val value: String) : MetadataEvent
    }

    /** Events that add, remove, or change the active edit target in the player list. */
    sealed interface PlayerListEvent : AddPlayEvent {

        /** User tapped "add player" with no suggestion; inserts a blank row. */
        data class AddEmptyPlayer(
            val startPosition: Int
        ) : PlayerListEvent

        /** User tapped a suggested player to add them at [startPosition]. */
        data class AddPlayerFromSuggestion(
            val playerId: PlayerIdentity,
            val startPosition: Int
        ) : PlayerListEvent

        /** User removed the player identified by [playerEntryId] from the list. */
        data class RemovePlayer(
            val playerEntryId: PlayerIdentity
        ) : PlayerListEvent

        /** User tapped a player row to open its inline edit panel. */
        data class EditPlayer(
            val playerEntryId: PlayerIdentity
        ) : PlayerListEvent

        /** User dismissed the inline player-edit panel. */
        data object StopEditingPlayer : PlayerListEvent
    }

    /** Events that edit a specific player's identity fields. */
    sealed interface PlayerEditEvent : AddPlayEvent {

        /** User changed the display name of [playerEntryId]. */
        data class NameChanged(
            val playerEntryId: PlayerIdentity,
            val name: String
        ) : PlayerEditEvent

        /** User changed the BGG username of [playerEntryId]. */
        data class UsernameChanged(
            val playerEntryId: PlayerIdentity,
            val username: String
        ) : PlayerEditEvent

        /** User changed the team label of [playerEntryId]. */
        data class TeamChanged(
            val playerEntryId: PlayerIdentity,
            val team: String
        ) : PlayerEditEvent
    }

    /** Events that modify a player's score or win flag. */
    sealed interface PlayerScoreEvent : AddPlayEvent {

        /** User updated the numeric score for [playerEntryId]. */
        data class ScoreChanged(
            val playerEntryId: PlayerIdentity,
            val score: Int
        ) : PlayerScoreEvent

        /** User toggled the winner status for [playerEntryId]. */
        data class WinnerToggled(
            val playerEntryId: PlayerIdentity,
            val isWinner: Boolean
        ) : PlayerScoreEvent
    }

    /** Events related to the colour picker for a player. */
    sealed interface PlayerColorEvent : AddPlayEvent {

        /** User opened the colour picker for [playerEntryId]. */
        data class ColorClicked(
            val playerEntryId: PlayerIdentity
        ) : PlayerColorEvent

        /** User picked [color] for [playerEntryId]. */
        data class ColorSelected(
            val playerEntryId: PlayerIdentity,
            val color: PlayerColor
        ) : PlayerColorEvent
    }

    /** Events that trigger suggestion refreshes. */
    sealed interface SuggestionEvent : AddPlayEvent {

        /** Request a fresh load of player suggestions for the current location. */
        data object RefreshPlayerSuggestions : SuggestionEvent
    }

    /** Primary action events emitted by the screen's action buttons. */
    sealed interface ActionEvent : AddPlayEvent {

        /** User tapped Save; the ViewModel should validate and persist the play. */
        data object SaveClicked : ActionEvent

        /** User tapped Cancel; the ViewModel should discard changes and navigate back. */
        data object CancelClicked : ActionEvent
    }
}
