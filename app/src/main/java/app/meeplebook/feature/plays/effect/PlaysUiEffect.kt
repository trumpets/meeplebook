package app.meeplebook.feature.plays.effect

import app.meeplebook.core.plays.model.PlayId
import app.meeplebook.core.ui.UiText

/**
 * One-shot UI effects emitted by the Plays feature.
 *
 * These are collected from a `SharedFlow` and must not be modeled inside
 * [app.meeplebook.feature.plays.PlaysUiState], because they represent transient work such as
 * navigation or snackbars.
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
