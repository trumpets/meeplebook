package app.meeplebook.feature.plays.reducer

import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import javax.inject.Inject

/**
 * Reducer for the reducer-owned [PlaysBaseState].
 *
 * This reducer handles only synchronous state mutation. It does not decide navigation, refresh
 * work, or display-state derivation; those concerns live in the effect producer and mapper layer.
 */
class PlaysReducer @Inject constructor() {

    /**
     * Applies a single [event] to the current reducer-owned [state].
     *
     * @return The next [PlaysBaseState] after handling the event.
     */
    fun reduce(
        state: PlaysBaseState,
        event: PlaysEvent
    ): PlaysBaseState =
        when (event) {
            is PlaysEvent.SearchChanged -> state.copy(searchQuery = event.query)
            is PlaysEvent.ActionEvent -> state
        }
}
