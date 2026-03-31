package app.meeplebook.feature.addplay

import app.meeplebook.core.plays.domain.PlayerIdentity
import app.meeplebook.core.plays.model.PlayerColor
import java.time.Instant

enum class OptionalField { QUANTITY, INCOMPLETE, COMMENTS }

/** Top-level union of all user-driven events on the Add Play screen. */
sealed interface AddPlayEvent {

    /**
     * Events for Path 1 (entering the screen without a pre-selected game).
     *
     * The screen starts in game-search mode when no gameId is provided; once the user
     * selects a game the full form becomes visible.
     */
    sealed interface GameSearchEvent : AddPlayEvent {

        /** User changed the text in the game-search field. */
        data class GameSearchQueryChanged(val query: String) : GameSearchEvent

        /** User selected a game from the search results. */
        data class GameSelected(val gameId: Long, val gameName: String) : GameSearchEvent
    }


    /** Events that modify the top-level play metadata (date, duration, location). */
    sealed interface MetadataEvent : AddPlayEvent {

        /** User changed the play date. */
        data class DateChanged(val date: Instant) : MetadataEvent

        /** User changed the play duration. [minutes] is `null` when the field is cleared. */
        data class DurationChanged(val minutes: Int?) : MetadataEvent

        /** User typed in the location field. */
        data class LocationChanged(val value: String) : MetadataEvent

        /** User changed the play quantity. [value] is `null` when the field is cleared. */
        data class QuantityChanged(val value: Int?) : MetadataEvent

        /** User toggled the incomplete flag. */
        data class IncompleteToggled(val value: Boolean) : MetadataEvent

        /** User changed the play comments. */
        data class CommentsChanged(val value: String) : MetadataEvent

        /** User tapped an optional field in the FAB menu to make it visible. */
        data class ShowOptionalField(val field: OptionalField) : MetadataEvent
    }

    /** Events that add, remove, or change the active edit target in the player list. */
    sealed interface PlayerListEvent : AddPlayEvent {

        /** User tapped "add player" with no suggestion; inserts a new row. */
        data class AddNewPlayer(
            val playerName: String,
            val startPosition: Int
        ) : PlayerListEvent

        /** User tapped a suggested player to add them at [startPosition]. */
        data class AddPlayerFromSuggestion(
            val playerIdentity: PlayerIdentity,
            val startPosition: Int
        ) : PlayerListEvent

        /** User removed the player identified by [playerIdentity] from the list. */
        data class RemovePlayer(
            val playerIdentity: PlayerIdentity
        ) : PlayerListEvent

        /** User reordered the player from [fromIndex] to [toIndex] via drag-to-reorder. */
        data class PlayerReordered(
            val fromIndex: Int,
            val toIndex: Int,
        ) : PlayerListEvent

        /**
         * User tapped "Undo" after a swipe-to-delete.
         * Restores [player] at [atIndex] in the list.
         */
        data class RestorePlayer(
            val player: PlayerEntryUi,
            val atIndex: Int,
        ) : PlayerListEvent

        /** User tapped a player row to open its inline edit panel. */
        data class EditPlayer(
            val playerIdentity: PlayerIdentity
        ) : PlayerListEvent

        /** User dismissed the inline player-edit panel. */
        data object StopEditingPlayer : PlayerListEvent
    }

    /** Events that edit a specific player's identity fields. */
    sealed interface PlayerEditEvent : AddPlayEvent {

        /** User changed the display name of [playerIdentity]. */
        data class NameChanged(
            val playerIdentity: PlayerIdentity,
            val name: String
        ) : PlayerEditEvent

        /** User changed the BGG username of [playerIdentity]. */
        data class UsernameChanged(
            val playerIdentity: PlayerIdentity,
            val username: String
        ) : PlayerEditEvent

        /** User changed the team label of [playerIdentity]. */
        data class TeamChanged(
            val playerIdentity: PlayerIdentity,
            val team: String
        ) : PlayerEditEvent
    }

    /** Events that modify a player's score or win flag. */
    sealed interface PlayerScoreEvent : AddPlayEvent {

        /** User updated the numeric score for [playerIdentity]. */
        data class ScoreChanged(
            val playerIdentity: PlayerIdentity,
            val score: Int
        ) : PlayerScoreEvent

        /** User toggled the winner status for [playerIdentity]. */
        data class WinnerToggled(
            val playerIdentity: PlayerIdentity,
            val isWinner: Boolean
        ) : PlayerScoreEvent
    }

    /** Events related to the colour picker for a player. */
    sealed interface PlayerColorEvent : AddPlayEvent {

        /** User opened the colour picker for [playerIdentity]. */
        data class ColorClicked(
            val playerIdentity: PlayerIdentity
        ) : PlayerColorEvent

        /** User picked [color] for [playerIdentity]. */
        data class ColorSelected(
            val playerIdentity: PlayerIdentity,
            val color: PlayerColor
        ) : PlayerColorEvent
    }

    /** Primary action events emitted by the screen's action buttons. */
    sealed interface ActionEvent : AddPlayEvent {

        /** User tapped Save; the ViewModel should validate and persist the play. */
        data object SaveClicked : ActionEvent

        /** User tapped Cancel; the ViewModel should discard changes and navigate back. */
        data object CancelClicked : ActionEvent
    }
}
