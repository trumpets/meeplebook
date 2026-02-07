package app.meeplebook.feature.addplay

import app.meeplebook.core.collection.model.GameSummary
import app.meeplebook.core.plays.model.PlayerHistory
import app.meeplebook.core.ui.UiText
import java.time.Instant

/**
 * UI state for the Add Play screen.
 */
sealed interface AddPlayUiState {

    /** Common state shared across all UI state variants. */
    val common: AddPlayCommonState

    /** Initial loading state. */
    data object Loading : AddPlayUiState {
        override val common = AddPlayCommonState()
    }

    /**
     * Form state where user can enter play details.
     */
    data class Form(
        val formState: AddPlayFormState,
        override val common: AddPlayCommonState
    ) : AddPlayUiState

    /**
     * Saving state while the play is being saved.
     */
    data class Saving(
        val formState: AddPlayFormState,
        override val common: AddPlayCommonState
    ) : AddPlayUiState

    /**
     * Error state if saving failed.
     */
    data class Error(
        val errorMessageUiText: UiText,
        val formState: AddPlayFormState,
        override val common: AddPlayCommonState
    ) : AddPlayUiState
}

/**
 * Common state shared across all [AddPlayUiState] variants.
 */
data class AddPlayCommonState(
    val dummy: Boolean = false // Placeholder for future common properties
)

/**
 * The form state containing all the input fields.
 */
data class AddPlayFormState(
    // Game selection
    val selectedGame: GameSummary? = null,
    val gameSearchQuery: String = "",
    val gameSearchResults: List<GameSummary> = emptyList(),
    val isSearchingGames: Boolean = false,

    // Basic play info
    val date: Instant = Instant.now(),
    val duration: String = "", // minutes as string for easier input
    val location: String = "",
    val comments: String = "",

    // Players
    val players: List<PlayerFormItem> = emptyList(),
    val expandedPlayerId: String? = null, // ID of player item with expanded details

    // Player addition
    val isAddingPlayer: Boolean = false,
    val newPlayerName: String = "",
    val suggestedPlayers: List<PlayerHistory> = emptyList(),

    // Color picker
    val showColorPickerForPlayer: String? = null, // Player ID
    val colorHistory: List<String> = emptyList(), // Recently used colors

    // Validation
    val errors: AddPlayFormErrors = AddPlayFormErrors()
)

/**
 * Represents a player in the form.
 */
data class PlayerFormItem(
    val id: String, // Temporary UUID for UI
    val name: String,
    val username: String? = null,
    val userId: Long? = null,
    val startPosition: String = "",
    val color: String? = null,
    val score: String = "", // as string for easier input
    val win: Boolean = false,
    val team: String = ""
)

/**
 * Validation errors for the form.
 */
data class AddPlayFormErrors(
    val gameError: UiText? = null,
    val dateError: UiText? = null,
    val durationError: UiText? = null,
    val playersError: UiText? = null
)

/**
 * One-time UI effects for the Add Play screen.
 */
sealed interface AddPlayUiEffects {
    /**
     * Navigate back after successfully saving.
     */
    data object NavigateBack : AddPlayUiEffects

    /**
     * Show a snackbar message.
     */
    data class ShowSnackbar(val messageUiText: UiText) : AddPlayUiEffects
}
