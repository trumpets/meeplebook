package app.meeplebook.feature.collection.reducer

import app.meeplebook.core.ui.architecture.Reducer
import app.meeplebook.feature.collection.CollectionBaseState
import app.meeplebook.feature.collection.CollectionEvent
import javax.inject.Inject

/**
 * Reducer for persistent display-related Collection state.
 *
 * This reducer owns reducer-visible presentation choices such as:
 * - list vs grid mode
 * - selected sort order
 * - sort-sheet visibility
 */
class CollectionDisplayReducer @Inject constructor() : Reducer<CollectionBaseState, CollectionEvent> {
    override fun reduce(
        state: CollectionBaseState,
        event: CollectionEvent
    ): CollectionBaseState {
        return when(event) {
            is CollectionEvent.DisplayEvent.ViewModeSelected -> {
                state.copy(viewMode = event.viewMode)
            }

            is CollectionEvent.DisplayEvent.SortSelected -> {
                state.copy(
                    sort = event.sort,
                    isSortSheetVisible = false
                )
            }

            CollectionEvent.SortSheetEvent.OpenSortSheet -> {
                state.copy(isSortSheetVisible = true)
            }

            CollectionEvent.SortSheetEvent.DismissSortSheet -> {
                state.copy(isSortSheetVisible = false)
            }

            else -> state
        }
    }
}
