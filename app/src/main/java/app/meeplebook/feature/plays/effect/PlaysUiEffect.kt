package app.meeplebook.feature.plays.effect

import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.ui.UiText

/**
 * One-time UI effects for the Plays screen.
 *
 * Unlike [app.meeplebook.feature.plays.PlaysUiState], which represents the continuous state of the screen,
 * UI effects are one-time events that trigger side effects such as navigation,
 * scrolling, or showing dialogs. Effects are emitted via a SharedFlow and consumed
 * once by the UI layer.
 */
sealed interface PlaysUiEffect {

    /**
     * Navigate to the details screen for a specific play.
     *
     * @property playId The ID of the play to view.
     */
    data class NavigateToPlay(val playId: PlayId) : PlaysUiEffect

    /**
     * Show a temporary snackbar message to the user.
     *
     * @property messageUiText The message to display.
     */
    data class ShowSnackbar(val messageUiText: UiText) : PlaysUiEffect
}
