package app.meeplebook.feature.addplay

import app.meeplebook.core.ui.UiText

/**
 * UI-level side effects produced by the Add Play feature.
 *
 * These are handled by the Composable screen (e.g., via a `SharedFlow` collected
 * with `LaunchedEffect`).  They represent one-shot interactions with the UI layer
 * such as navigation transitions or transient feedback messages.
 *
 * @see AddPlayEffect for effects that are handled by the ViewModel.
 */
sealed interface AddPlayUiEffect {

    /**
     * Navigate back to the previous screen.
     *
     * Produced when the user taps Cancel or after a play is saved successfully.
     */
    data object NavigateBack : AddPlayUiEffect

    /**
     * Show a transient error message to the user (e.g., via a Snackbar).
     *
     * Produced when validation fails on a Save attempt.
     */
    data class ShowError(val message: UiText) : AddPlayUiEffect
}
