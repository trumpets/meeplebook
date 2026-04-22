package app.meeplebook.feature.overview.reducer

import app.meeplebook.feature.overview.OverviewBaseState
import app.meeplebook.feature.overview.OverviewEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewReducerTest {

    @Test
    fun `reducer leaves state unchanged for refresh event`() {
        val reducer = OverviewReducer()
        val state = OverviewBaseState()

        val newState = reducer.reduce(state, OverviewEvent.ActionEvent.Refresh)

        assertEquals(state, newState)
    }
}
