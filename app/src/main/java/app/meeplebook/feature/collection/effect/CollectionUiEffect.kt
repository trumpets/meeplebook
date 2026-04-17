package app.meeplebook.feature.collection.effect

import app.meeplebook.core.ui.UiText
import app.meeplebook.feature.collection.CollectionViewMode

/**
 * One-shot UI effects for the Collection screen.
 *
 * These effects are emitted from the ViewModel through a `SharedFlow` and consumed once by the UI.
 * Persistent UI state such as sort-sheet visibility lives in reducer-owned base state instead.
 */
sealed interface CollectionUiEffect {
    data class ScrollToIndex(val viewMode: CollectionViewMode, val index: Int) : CollectionUiEffect
    data class NavigateToGame(val gameId: Long) : CollectionUiEffect
    data class ShowSnackbar(val messageUiText: UiText) : CollectionUiEffect
}
