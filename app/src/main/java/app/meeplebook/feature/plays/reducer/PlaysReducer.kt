package app.meeplebook.feature.plays.reducer

import app.meeplebook.feature.plays.PlaysBaseState
import app.meeplebook.feature.plays.PlaysEvent
import javax.inject.Inject

class PlaysReducer @Inject constructor() {

    fun reduce(
        state: PlaysBaseState,
        event: PlaysEvent
    ): PlaysBaseState =
        when (event) {
            is PlaysEvent.SearchChanged -> state.copy(searchQuery = event.query)
            is PlaysEvent.ActionEvent -> state
        }
}
